package com.black.shuang.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Client {
	//������־/��Ծ״̬
	private static final String ALIVE = "_ALIVE_";
	//�Ͽ���־
	private static final String DEAD = "_DEAD_";
	//�ͻ���״̬
	private String clientState = ALIVE;
	//�����״̬,Ĭ���ѶϿ�
	private String serverState = DEAD;
	
	private String ip;
	private int port;
	private Socket socket;
	
	public Client(String ip, int port) throws UnknownHostException, IOException {
		this.ip = ip;
		this.port = port;
		connect();
	}

	public void connect(){
		new Thread() {
			public void run() {
				while(true){
//					System.out.println(serverState);
					if(DEAD.equals(serverState) && socket == null) {
						try {
							socket = new Socket();
							socket.connect(new InetSocketAddress(ip, port), 10000);
							serverState = ALIVE;
							System.out.println("���ӳɹ�");
						} catch (IOException e) {
							System.out.println("���ӳ�ʱ");
						}
					}
				}
			}
		}.start();
	}
	public void start(){
		//���������߳�
		receiveMessage();
		//���������߳�
//		sendMessage();
	}

	private void receiveMessage() {
		new Thread("receiveMessage"){
			public void run() {
				BufferedReader br=null;
				while(true){
					try {
						if(socket !=null && DEAD.equals(serverState)) {
							br = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
						}else {
							continue;
						}
						String message = null;
						socket.setSoTimeout(10000);
						message = br.readLine();
						if(!"_OK_".equals(message)){
							System.out.println(message);
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch(SocketException e){
						System.out.println("�������ѹر�");
					}catch(SocketTimeoutException e){
						System.out.println("��ȡ��ʱ��");
					}catch (IOException e) {
						e.printStackTrace();
					}finally{
						serverState = DEAD;
						try {
							if(br!=null){
								br.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}.start();
	}

	private void sendMessage() {
		new Thread("sendMessage"){
			@Override
			public void run() {
				Scanner scanner = null;
				PrintWriter pw = null;
				Thread aliveThread;
				aliveThread = alive(pw);
				while(ALIVE.equals(serverState)){
					try {
						if(pw == null) {
							pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
						}
						System.out.print("�������뷢�͵���Ϣ��");
						scanner = new Scanner(System.in);
						String message = scanner.nextLine();
						pw.println(message);
						pw.flush();
						if("bye".equals(message)) {
							clientState = DEAD;
							aliveThread.interrupt();
							break;
						}
						System.out.println("ִ�����");
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}finally{
						scanner.close();
						pw.close();
					}
				}
			}
		}.start();;
	}
	/**
	 * ��ʱ����������ȷ���Ƿ�����
	 * @param pw
	 * @return 
	 */
	private Thread alive(PrintWriter pw) {
		Thread t = new Thread() {
			public void run() {
				while(!isInterrupted() && ALIVE.equals(clientState)) {
					try {
						//ÿ��5�뷢��һ�Ρ��һ����ߡ��ı�־
						TimeUnit.SECONDS.sleep(5);
						pw.println(ALIVE);
						pw.flush();
					} catch (InterruptedException e) {
						//�ݲ�������
						break;
					}
				}
			}
		};
		t.start();
		return t;
	}

	public String getState() {
		return clientState;
	}

	public void setState(String state) {
		this.clientState = state;
	}
	
	public static void main(String[] args) {
		Client c;
		try {
			c = new Client("127.0.0.1",9999);
			c.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
