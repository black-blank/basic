package com.black.shuang;

import java.util.concurrent.TimeUnit;

public class Thread_1 {

	public static void main(String[] args) throws InterruptedException {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					while(true) {
						TimeUnit.SECONDS.sleep(1);
						System.out.println("while2");
					}
				} catch (InterruptedException e) {
					//System.out.println("sorry,I am catched:"+Thread.currentThread().isInterrupted());
				}
			}
		};
		t.setDaemon(true);
		t.start();
		TimeUnit.SECONDS.sleep(2);
		System.out.println("a:"+t.isInterrupted());
		t.interrupt();
//		TimeUnit.SECONDS.sleep(2);
		System.out.println("b:"+t.isInterrupted());
		
	}
}
