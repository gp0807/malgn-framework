package malgnsoft.util;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;

public class Gmail {

	protected String id = null;
	protected String pw = null;
	protected String mailFrom = null;
	protected String host = "smtp.gmail.com";
	protected int port = 465;
	protected boolean ssl = true;
	protected String encoding = "utf-8";

	public Gmail(String id, String pw) {
		this.id = id;
		this.pw = pw;
		this.mailFrom = id;
	}

	public void setFrom(String from) {
		this.mailFrom = from;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setSSL(boolean flag) {
		this.ssl = flag;
	}

	public void setEncoding(String enc) {
		this.encoding = enc;
	}
		
	protected Session getSession() {
		Properties prop = new Properties();
		prop.put("mail.smtp.host", this.host);
		prop.put("mail.smtp.port", String.valueOf(this.port));
		prop.put("mail.smtp.auth", "true");

		if(this.ssl) {
			prop.put("mail.smtp.starttls.enable","true");
			prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
		}

		SmtpAuthenticator auth = new SmtpAuthenticator(this.id, this.pw);
		return Session.getInstance(prop, auth);
	}

	public void send(String mailTo, String subject, String body) throws Exception {
		send(new String[] { mailTo }, subject, body, null);
	}

	public void send(String mailTo, String subject, String body, String[] files) throws Exception {
		send(new String[] { mailTo }, subject, body, files);
	}

	public void send(String[] mailTo, String subject, String body) throws Exception {
		send(mailTo, subject, body, null);
	}

	public void send(String[] mailTo, String subject, String body, String[] files) throws Exception {

		MimeMessage msg = new MimeMessage(getSession());
		InternetAddress from = new InternetAddress(mailFrom);
		if(!"".equals(from.getPersonal())) from.setPersonal(from.getPersonal(), encoding);
		InternetAddress[] to = new InternetAddress[mailTo.length];
		for(int i=0; i<mailTo.length; i++) {
			to[i] = new InternetAddress(mailTo[i]);
			if(!"".equals(to[i].getPersonal())) to[i].setPersonal(to[i].getPersonal(), encoding);
		}

		msg.setFrom(from);
		msg.setRecipients(Message.RecipientType.TO, to);
		msg.setSubject(subject, encoding);
		msg.setSentDate(new Date());

		if(files == null) {
			msg.setContent(body, "text/html; charset=" + encoding);
		} else {
			for(int i=0; i<files.length; i++) {
				MimeBodyPart mbp1 = new MimeBodyPart();
				mbp1.setContent(body, "text/html; charset=" + encoding);
				MimeBodyPart mbp2 = new MimeBodyPart();

				FileDataSource fds = new FileDataSource(files[i]);
				mbp2.setDataHandler(new DataHandler(fds));
				mbp2.setFileName(fds.getName());

				Multipart mp = new MimeMultipart();
				mp.addBodyPart(mbp1);
				mp.addBodyPart(mbp2);

				msg.setContent(mp);
			}
		}

		Transport.send(msg);

	}

	private static class SmtpAuthenticator extends Authenticator {

		private String id = null;
		private String pw = null;

		public SmtpAuthenticator(String id, String pw) {
			this.id = id;
			this.pw = pw;
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(id, pw);
		}

	}

}