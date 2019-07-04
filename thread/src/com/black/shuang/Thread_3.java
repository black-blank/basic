package com.black.shuang;

import java.util.concurrent.TimeUnit;

import com.black.shuang.Thread_3.MyThread;

public class Thread_3 {
	
	public static void main(String[] args) throws InterruptedException {
		Thread_3 t3 = new Thread_3();
		
		MyThread myThread = t3.new MyThread();
		System.out.println("Ïß³ÌÆô¶¯...");
		myThread.start();
		TimeUnit.SECONDS.sleep(5);
		System.out.println("main end");
		myThread.close();
	}
	public class MyThread extends  Thread{
		private volatile boolean isClose = false;
		public void run() {
			int n = 0;
			while(!isClose && !isInterrupted()) {
				try {
					System.out.println("hello-"+n++);
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					System.out.println("i am catched");
				}
			}
		}

		public void close() {
			isClose = true;
			this.interrupt();
		}
	}
}
