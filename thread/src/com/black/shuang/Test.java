package com.black.shuang;

import java.util.concurrent.TimeUnit;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		Thread t = new Thread() {
			@Override
			public void run() {
				System.out.println("run:"+Thread.currentThread().getName());
			}
		};
		t.start();
		System.out.println(Thread.currentThread().getName());
	}

}
