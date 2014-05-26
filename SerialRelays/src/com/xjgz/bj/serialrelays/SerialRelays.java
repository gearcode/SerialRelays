package com.xjgz.bj.serialrelays;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * 串口继电器类
 * 
 * @author 李腾
 * 
 */
public class SerialRelays {

	private String serialName;
	
	private CommPortIdentifier portId;
	private SerialPort port;
	
	//串口设备输入输出流
	private OutputStream os;
	private InputStream is;
	
	//继电器状态监听器
	private SerialRelaysStatusListener statusListener;

	/**
	 * 串口名称
	 * 
	 * @param serialName
	 * @throws NoSuchPortException 
	 */
	public SerialRelays(String serialName, SerialRelaysStatusListener statusListener) throws NoSuchPortException {
		this.serialName = serialName;
		this.statusListener = statusListener;
		//获取到该串口
		this.portId = CommPortIdentifier.getPortIdentifier(serialName);
	}
	
	public SerialRelays(String serialName) throws NoSuchPortException {
		this(serialName, null);
	}

	public String getSerialName() {
		return serialName;
	}
	
	public SerialRelaysStatusListener getStatusListener() {
		return statusListener;
	}

	/**
	 * 打开该串口, 获得控制权
	 * 在串口设备打开后会自动返回继电器状态, 需要自己实现SerialRelaysStatusListener
	 * 
	 * @return SerialRelays
	 * @throws PortInUseException 
	 * @throws UnsupportedCommOperationException 
	 * @throws IOException 
	 * @throws TooManyListenersException 
	 */
	public SerialRelays open() throws PortInUseException, UnsupportedCommOperationException, IOException, TooManyListenersException {
		this.port = (SerialPort) portId.open("BarrierPort", 2000);
		this.port.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

		//获取输入输出流
		this.os = this.port.getOutputStream();
		this.is = this.port.getInputStream();

		//监听输出数据
		port.addEventListener(new SerialEventListener(is, statusListener));
		port.notifyOnDataAvailable(true);

		//发送返回继电器状态的请求
		os.write(new byte[]{0x55, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x56});
		
		return this;
	}
	
	/**
	 * 关闭该串口
	 * 
	 * @return SerialRelays
	 */
	public SerialRelays close() {
		try {
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.port.close();
		return this;
	}
	
	/**
	 * 闭合第i个继电器
	 * @param i 继电器下标, 从0开始
	 * @return SerialRelays
	 */
	public synchronized SerialRelays openRelays(int i) {
		byte[] bytes = new byte[]{0x55, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x59};
		if(this.os != null) {
			bytes[i+3] = 0x02;
			try {
				os.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	/**
	 * 断开第i个继电器
	 * @param i 继电器下标, 从0开始
	 * @return SerialRelays
	 */
	public synchronized SerialRelays closeRelays(int i) {
		byte[] bytes = new byte[]{0x55, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x58};
		if(this.os != null) {
			bytes[i+3] = 0x01;
			try {
				os.write(bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this;
	}
	
	public SerialRelays pointRelays(final int i) {
//		new Thread(new Runnable() {
//			public void run() {
//				openRelays(i);
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				closeRelays(i);
//			}
//		}).start();
		openRelays(i);
		closeRelays(i);
		return this;
	}
	
	public static void main(String[] args) {
		try {
			new SerialRelays("COM10", new SerialRelaysStatusListener() {
				public void relaysStatus(boolean[] status) {
					System.out.println(Arrays.toString(status));
				}
			}).open().pointRelays(0).pointRelays(1).pointRelays(2).pointRelays(3);
		} catch (NoSuchPortException e) {
			e.printStackTrace();
		} catch (PortInUseException e) {
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
	}
}

/**
 * 串口状态监听类, 用于监听串口继电器的状态改变
 * @author 李腾
 *
 */
class SerialEventListener implements SerialPortEventListener {
	private InputStream is;
	private SerialRelaysStatusListener statusListener;
	
	public SerialEventListener(InputStream is, SerialRelaysStatusListener statusListener) {
		this.is = is;
		this.statusListener = statusListener;
	}
	
	public void serialEvent(SerialPortEvent spe) {
		try {
			if(is.available() >= 8) {
				byte[] bytes = new byte[8];
				is.read(bytes);
				//判断数据合法性
				if(bytes[0] == 34 && bytes[0] + bytes[1] + bytes[2] + bytes[3] + bytes[4] + bytes[5] + bytes[6] == bytes[7]) {
					//调用继电器状态监听方法
					if(statusListener != null) {
						statusListener.relaysStatus(new boolean[]{
							bytes[3] == 2,
							bytes[4] == 2,
							bytes[5] == 2,
							bytes[6] == 2,
						});
					}	
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}