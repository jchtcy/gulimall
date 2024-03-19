package com.jch.gulimall.thirdparty.service.impl;

import com.jch.gulimall.thirdparty.service.SmsService;
import com.sun.mail.util.MailSSLSocketFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 短信服务实现类
 * @Author: wanzenghui
 * @Date: 2021/11/27 22:58
 */
@Component
public class SmsServiceImpl implements SmsService {

    @Value("${spring.mail.default-encoding}")
    String defaultEncoding;

    @Value("${spring.mail.username}")
    String username;

    @Value("${spring.mail.password}")
    String password;

    @Value("${spring.mail.host}")
    String host;

    //发送验证码邮件
    @Override
    public Boolean sendCode(String to, String vcode) throws GeneralSecurityException {
        //1.创建连接对象，连接到邮箱服务器
        Properties props = System.getProperties();
        //设置邮件服务器
        props.setProperty("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
        //SSL加密
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        props.put("mail.smtp.ssl.enable","true");
        props.put("mail.smtp.ssl.socketFactory", sf);
        //props：用来设置服务器地址，主机名；Authenticator：认证信息
        Session session = Session.getDefaultInstance(props,new Authenticator() {
            @Override
            //通过密码认证信息
            protected PasswordAuthentication getPasswordAuthentication() {
                //new PasswordAuthentication(用户名, password);
                //这个用户名密码就可以登录到邮箱服务器了,用它给别人发送邮件
                return new PasswordAuthentication(username,password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            //2.1设置发件人：
            message.setFrom(new InternetAddress(username));
            //2.2设置收件人 这个TO就是收件人
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            //2.3邮件的主题
            message.setSubject("谷粒商城验证码邮件");
            //2.4设置邮件的正文 第一个参数是邮件的正文内容 第二个参数是：是文本还是html的连接
            message.setContent("<h1>谷粒商城验证码邮件,请接收你的验证码：</h1><h3>你的验证码是："+vcode+"，请妥善保管好你的验证码！</h3>", "text/html;charset=" + defaultEncoding);
            //3.发送一封激活邮件
            Transport.send(message);
        }catch(MessagingException mex){
            mex.printStackTrace();
        }
        return true;
    }
}
