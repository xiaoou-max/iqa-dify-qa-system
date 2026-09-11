package com.frml.api.vo;

public class APIHttpStatus {
	public static final int OK = 200; //服务器返回用户请求的数据，该操作是幂等的，用于get请求
	public static final int CREATED = 201; //新建或者修改数据成功,用于post或put请求
	public static final int NOT_CONTENT  = 204; //删除数据成功,用于delete请求
	public static final int BAD_REQUEST  = 400; //用户发出的请求有问题，该操作是幂等的
	public static final int Unauthoried  = 401; //表示用户没有认证，无法进行操作
	public static final int Forbidden = 403; //用户访问是被禁止的
	public static final int NOT_FOUND = 404; //客户端请求了一个不存在的资源或资源集合，服务端对此不作任何动作（此行为幂等）。
	public static final int Unprocesable_Entity  = 422; //当创建一个对象时，发生一个验证错误
	public static final int INTERNAL_SERVER_ERROR = 500; //服务器遇到错误，无法完成请求
	public static final int Service_Unavailable = 503; //服务不可用状态，多半是因为服务器问题，例如CPU占用率大，等等
}
