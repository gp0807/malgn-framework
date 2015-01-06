package malgnsoft.util;

import sun.misc.BASE64Encoder; 
import sun.misc.BASE64Decoder;

import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class AES128 {

	private String encoding = "utf-8";
	private String keyPath = null;
    private byte[] keyValue = new byte[] { 'X', 'h', 'a', 's', 'I', 's', 'A', 'B', 'e', 'y', 'r', 'w', 'z', 'K', 'e', 'p' };

	public AES128() {

	}

	public AES128(String path) {
		keyPath = path;
	}

	public void setEncoding(String enc) {
		encoding = enc;
	}

	public void setKey(byte[] key) {
		keyValue = key;
	}

	public void setKey(String password) throws Exception {
		byte[] passwordBytes = password.getBytes(encoding);
		int len = passwordBytes.length;
		if (len >= 16) {
			System.arraycopy(passwordBytes, 0, keyValue, 0, 16);
		} else {
			System.arraycopy(passwordBytes, 0, keyValue, 0, len);
			for (int i = 0; i < (16 - len); i++) {
				keyValue[len + i] = passwordBytes[i % len];
			}
		}
	}

	public String getKeyValue() throws Exception {
		return new String(keyValue, encoding);
	}

	public void setKeyPath(String path) {
		keyPath = path;
	}

	public SecretKeySpec getKeySpec() throws Exception { 
		if(null != keyPath) {
			File f = new File(keyPath);
			if (!f.exists()) createKeyFile();
			new FileInputStream(f).read(keyValue);
		}
		return new SecretKeySpec(keyValue, "AES");
	}

	public void createKeyFile() throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		kgen.init(128);
		SecretKey key = kgen.generateKey();
		byte[] bytes = key.getEncoded();

		File f = new File(keyPath);
		if(f.exists()) Malgn.copyFile(keyPath, keyPath + "." + System.currentTimeMillis() + ".bak");
		new FileOutputStream(f).write(bytes);
	}

	public String encrypt(String text) throws Exception {
		SecretKeySpec spec = getKeySpec();
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, spec);
		BASE64Encoder enc = new BASE64Encoder();
		return enc.encode(cipher.doFinal(text.getBytes(encoding))).toString();
	}

	public String decrypt(String text) throws Exception {
		SecretKeySpec spec = getKeySpec();
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, spec);
		BASE64Decoder dec = new BASE64Decoder();
		return new String(cipher.doFinal(dec.decodeBuffer(text)), encoding);
	}

}