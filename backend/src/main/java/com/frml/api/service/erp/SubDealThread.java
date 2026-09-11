package com.frml.api.service.erp;

import java.util.LinkedHashMap;

import java.util.concurrent.CountDownLatch;


import com.frml.api.config.ApplicationContextProvider;

public class SubDealThread  extends Thread {
	
	
	private CountDownLatch countDownLatch;
	
	private LinkedHashMap<String, Object> excelParam;

	private String tablename;

	private ErpService erpService;

	public SubDealThread(CountDownLatch countDownLatch, LinkedHashMap<String, Object> excelParam, String tablename) {
		super();
		this.countDownLatch = countDownLatch;
		this.excelParam = excelParam;
		this.tablename = tablename;
		this.erpService = ApplicationContextProvider.getBean(ErpService.class);
	}



	/**
	 * 启动线程后执行活动实例的同步工作
	 */
	public void run() {
		try{
			this.erpService.dealForExcelParam(excelParam,tablename);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		} finally {
			countDownLatch.countDown();
		}
		
	}
}
