package com.xjgz.bj.serialrelays;

/**
 * 继电器状态监听器
 * 
 * @author 李腾
 *
 */
public interface SerialRelaysStatusListener {

	/**
	 * 当继电器状态发生改变时, 会调用此方法
	 * @param status 继电器的状态, true表示闭合, false表示断开
	 */
	public void relaysStatus(boolean[] status);
}
