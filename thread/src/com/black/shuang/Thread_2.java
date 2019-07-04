package com.black.shuang;

import java.util.concurrent.TimeUnit;

public class Thread_2 {

	public static void main(String[] args) {
		Thread t1 = new Thread() {
			public void run() {
				int n = 0;
				while(n < 10) {
					try {
						System.out.println("t1£º"+Thread.currentThread().getName());
						TimeUnit.SECONDS.sleep(1);
						n++;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread t2 = new Thread() {
			public void run() {
				try {
					t1.join();
					while(true) {
						System.out.println("t2£º"+Thread.currentThread().getName());
						TimeUnit.SECONDS.sleep(1);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		t1.start();
		t2.start();
		
	}
}
