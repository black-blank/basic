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
	
	private int port;//服务器端口
	private static ServerSocket serverSocket;
	private static List<SubClient> clients = Collections.synchronizedList(new LinkedList<>());//连接的客户端集合
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
				//接受一个客户端连接
				System.out.println("客户端连接中...");
				System.out.println("当前用户数："+clients.size());
				Socket socket = serverSocket.accept();
				subClient = new SubClient(socket,clientId.getAndIncrement());
				System.out.println(subClient.getHostName()+"已连接");
				clients.add(subClient);
				new Thread(subClient,"clientStart").start();
				
			}
			System.out.println("服务器已断开连接");
		} catch(SocketTimeoutException e){
			System.out.println(e.getMessage());
		}catch (IOException e) {
			System.out.println(subClient.getHostIp()+"已断开连接");
		}
	}

	/**
	 * 检查当前用户，并删除已退出的用户
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
	 * 广发消息
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
									pw.println(name+"说："+mess);
//									System.out.println(subClient.getHostName()+"说："+m.getMessage());
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
		//是否保持连接
		private Boolean isAlive;
		//客户端唯一编号
		private Integer id;
		//客户端IP地址
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
			this.hostName = "用户"+clientId;
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
						//正常退出
						System.out.println(hostName+"说："+message);
						break;
					}else{
						Message m = new Message();
						m.setId(id);
						m.setMessage(message);
						content.offer(m);
//						System.out.println(hostName+"说："+message);
					}
				}
				System.out.println(hostName+"：已正常断开连接");
			} catch (IOException e) {
				System.out.println(hostName+"：已异常断开连接");
			}finally{
				//客户端已退出
				this.isAlive = false;
				delete(this);
				System.out.println("当前用户数："+clients.size());
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
