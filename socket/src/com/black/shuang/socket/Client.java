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

import javax.management.monitor.Monitor;

public class Client {
	//心跳标志/活跃状态
	private static final String ALIVE = "_ALIVE_";
	//断开标志
	private static final String DEAD = "_DEAD_";
	//客户端状态
	private String clientState = ALIVE;
	//服务端状态,默认已断开
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
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(ip, port), 10000);
			socket.setSoTimeout(60000);
			serverState = ALIVE;
			System.out.println("连接成功");
		} catch (IOException e) {
			System.out.println("连接超时");
		}
	}
	
	public synchronized String getServerState() {
		return serverState;
	}
	public void start(){
		//启动接受线程
		receiveMessage();
		//启动发送线程
		sendMessage();
		
		monitor();
		
	}

	/**
	 * 监控服务器是否断开，断开则重连
	 */
	private void monitor() {
		new Thread() {
			public void run() {
				while(ALIVE.equals(clientState)) {
					if(DEAD.equals(getServerState())) {
						System.out.println("正在重新连接");
						try {
							TimeUnit.SECONDS.sleep(2);
							connect();
							if(ALIVE.equals(getServerState())) {
								receiveMessage();
								sendMessage();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}	
				}
			}
		}.start();
	}

	private void receiveMessage() {
		new Thread("receiveMessage"){
			public void run() {
				BufferedReader br=null;
					try {
						br = new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));
						while(true) {
							if(ALIVE.equals(getServerState())) {
								String message = null;
								message = br.readLine();
//								System.out.println(message);
								if("clientClosed-OK".equals(message)) {
									clientState = DEAD;
									return ;
								}
								if(!"_OK_".equals(message)){
									System.out.println(message);
								}
							}
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch(SocketException e){
						serverState = DEAD;
						System.out.println("服务器已关闭");
					}catch(SocketTimeoutException e){
						System.out.println("读取超时！");
					}catch (IOException e) {
						e.printStackTrace();
					}finally{
						try {
							if(br!=null){
								br.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
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
					try {
						pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
						aliveThread = alive(pw);
						while(true){
							if(ALIVE.equals(getServerState())) {
								System.out.print("请输入想发送的信息：");
								scanner = new Scanner(System.in);
								String message = scanner.nextLine();
								pw.println(message);
								pw.flush();
								if("bye".equals(message)) {
									clientState = DEAD;
									aliveThread.interrupt();
									break;
								}
							}
							System.out.println("执行完毕");
						}
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}finally{
						if(scanner!=null) {
							scanner.close();
						}
						if(pw!=null) {
							pw.close();
						}
					}
			}
		}.start();;
	}
	/**
	 * 定时发送心跳，确定是否在线
	 * @param pw
	 * @return 
	 */
	private Thread alive(PrintWriter pw) {
		Thread t = new Thread() {
			public void run() {
				while(!isInterrupted() && ALIVE.equals(clientState)) {
					try {
						//每隔5秒发送一次“我还在线”的标志
						TimeUnit.SECONDS.sleep(5);
						pw.println(ALIVE);
						pw.flush();
					} catch (InterruptedException e) {
						//暂不做处理
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
