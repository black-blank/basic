package com.black.shuang.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
	
	private int port;//�������˿�
	private static ServerSocket serverSocket;
	private static List<SubClient> clients = Collections.synchronizedList(new LinkedList<>());//���ӵĿͻ��˼���
	private static LinkedBlockingQueue<Message> content = new LinkedBlockingQueue<Message>();
	
	private volatile static AtomicInteger clientId = new AtomicInteger(1);
	
	public Server(int port) throws IOException {
		this.port = port;
		serverSocket = new ServerSocket(port);
	}

	public void start() {
		SubClient subClient = null;
		try {
			sendMessage();
			while(!serverSocket.isClosed()){
				//����һ���ͻ�������
				System.out.println("�ͻ���������...");
				System.out.println("��ǰ�û�����"+clients.size());
				Socket socket = serverSocket.accept();
				subClient = new SubClient(socket,clientId.getAndIncrement());
				System.out.println(subClient.getHostName()+"������");
				clients.add(subClient);
				new Thread(subClient,"clientStart").start();
				
			}
			System.out.println("�������ѶϿ�����");
		} catch(SocketTimeoutException e){
			System.out.println(e.getMessage());
		}catch (IOException e) {
			System.out.println(subClient.getHostIp()+"�ѶϿ�����");
		}
	}

	/**
	 * ��鵱ǰ�û�����ɾ�����˳����û�
	 * @param clients
	 */
	
	public static void delete(SubClient subClient) {
		synchronized (subClient) {
			if(!subClient.getIsAlive()){
				clients.remove(subClient);
			}
		}
	}
	
	/**
	 * �㷢��Ϣ
	 * @param cs
	 */
	public void sendMessage(){
		new Thread("sendMessageThread"){
			public void run() {
				synchronized (content) {
					while(true){
						Message m = null;
						while((m = content.poll()) != null){
							for (SubClient subClient : clients) {
								if(subClient.getId() != m.getId()){
									PrintWriter pw = subClient.getPw();
									String name = subClient.getHostName();
									String mess = m.getMessage();
									pw.println(name+"˵��"+mess);
//									System.out.println(subClient.getHostName()+"˵��"+m.getMessage());
									pw.flush();
								}
							}
							
						}
					}
				}
			}
		}.start();
	}
	
	static class SubClient implements Runnable{
		//�Ƿ񱣳�����
		private Boolean isAlive;
		//�ͻ���Ψһ���
		private Integer id;
		//�ͻ���IP��ַ
		private String hostIp;
		//
		private String hostName;
		
		private String message;
		private Socket socket;
		private PrintWriter pw;
		public SubClient(Socket socket, Integer clientId) {
			this.socket = socket;
			this.id = clientId;
			this.hostIp = socket.getInetAddress().getHostAddress();
//			this.hostName = socket.getInetAddress().getHostName();
			this.hostName = "�û�"+clientId;
		}
		@Override
		public void run() {
			BufferedReader br = null;
			try {
				InputStream inputStream = socket.getInputStream();
				br = new BufferedReader(new InputStreamReader(inputStream,"utf-8"));
				pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
				while(!"bye".equals(message)){
					message = br.readLine();
					if("_ALIVE_".equals(message)){
						pw.println("_OK_");
						pw.flush();
					}else if("bye".equals(message)){
						//�����˳�
						System.out.println(hostName+"˵��"+message);
						break;
					}else{
						Message m = new Message();
						m.setId(id);
						m.setMessage(message);
						content.offer(m);
//						System.out.println(hostName+"˵��"+message);
					}
				}
				System.out.println(hostName+"���������Ͽ�����");
			} catch (IOException e) {
				System.out.println(hostName+"�����쳣�Ͽ�����");
			}finally{
				//�ͻ������˳�
				this.isAlive = false;
				delete(this);
				System.out.println("��ǰ�û�����"+clients.size());
			}
		}
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public String getHostIp() {
			return hostIp;
		}
		public void setHostIp(String hostIp) {
			this.hostIp = hostIp;
		}
		public String getHostName() {
			return hostName;
		}
		public void setHostName(String hostName) {
			this.hostName = hostName;
		}
		public Socket getSocket() {
			return socket;
		}
		public void setSocket(Socket socket) {
			this.socket = socket;
		}
		public Boolean getIsAlive() {
			return isAlive;
		}
		public void setIsAlive(Boolean isAlive) {
			this.isAlive = isAlive;
		}
		public PrintWriter getPw() {
			return pw;
		}
		public void setPw(PrintWriter pw) {
			this.pw = pw;
		}
	}

	public static void main(String[] args) {
		Server socketServer;
		try {
			socketServer = new Server(9999);
			socketServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
