/*
 * The MIT License
 *
 * Copyright (c) 2016-2017 Marcelo "Ataxexe" Guimarães
 * <ataxexe@devnull.tools>
 *
 * ----------------------------------------------------------------------
 * Permission  is hereby granted, free of charge, to any person obtaining
 * a  copy  of  this  software  and  associated  documentation files (the
 * "Software"),  to  deal  in the Software without restriction, including
 * without  limitation  the  rights to use, copy, modify, merge, publish,
 * distribute,  sublicense,  and/or  sell  copies of the Software, and to
 * permit  persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this  permission  notice  shall be
 * included  in  all  copies  or  substantial  portions  of the Software.
 *                        -----------------------
 * THE  SOFTWARE  IS  PROVIDED  "AS  IS",  WITHOUT  WARRANTY OF ANY KIND,
 * EXPRESS  OR  IMPLIED,  INCLUDING  BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN  NO  EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM,  DAMAGES  OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT  OR  OTHERWISE,  ARISING  FROM,  OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE   OR   THE   USE   OR   OTHER   DEALINGS  IN  THE  SOFTWARE.
 */
package tools.devnull.jenkins.plugins.buildnotifications;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

/**
 * A class that represents a Telegram message
 *
 * @author Ataxexe
 */
public class TelegramMessage implements Message {

  private static final Logger LOGGER = Logger.getLogger(TelegramMessage.class.getName());

  private final String botToken;
  private final String chatIds;
  private final String tProxy;
  private final String tProxyUsr;
  private final String tProxyPwd;


  private String extraMessage;
  private String content;
  private String title;
  private String url;
  private String urlTitle;

  public TelegramMessage(String botToken, String chatIds, String extraMessage,
           String tProxy, String tProxyUsr, String tProxyPwd) {
    LOGGER.info("TelegramMessage()");
    this.botToken = botToken;
    this.chatIds = chatIds;
    this.extraMessage = extraMessage;
    this.tProxy = tProxy;
    this.tProxyUsr = tProxyUsr;
    this.tProxyPwd = tProxyPwd;
  }

  /**
   * Creates a new Telegram message based on the given parameters
   *
   * @param botToken the bot token
   * @param chatIds the target ids separated by commas (a group conversation id or a contact id)
   */
  public TelegramMessage(String botToken, String chatIds, String extraMessage) {
    this(botToken, chatIds, extraMessage, null, null, null);
  }

  @Override
  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public void setUrl(String url, String title) {
    this.url = url;
    this.urlTitle = title;
  }

  @Override
  public void highPriority() {
    // Not possible with Telegram
  }

  @Override
  public void normalPriority() {
    // Not possible with Telegram
  }

  @Override
  public void lowPriority() {
    // Not possible with Telegram
  }

  public boolean send() {
    String[] ids = chatIds.split("\\s*,\\s*");
    final CloseableHttpClient client;
    // set proxy
    boolean result = true;
    if (tProxy != null) {
      HttpClientBuilder clientBuilder = HttpClientBuilder.create();

      String[] split = tProxy.split(":");
      if (split.length == 2) {
        LOGGER.info("Try send via proxy " + tProxy);
        int proxyPort = Integer.parseInt(split[1]);
        String proxyHost = split[0];
        clientBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
        if (tProxyUsr != null && tProxyPwd != null) {
          BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
          credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(tProxyUsr, tProxyPwd));
          clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
      }
      client = clientBuilder.build();
    } else {
      client = HttpClients.createDefault();
    }

    try {
      for (String chatId : ids) {
        HttpPost post = new HttpPost(String.format("https://api.telegram.org/bot%s/sendMessage", botToken));
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.setEntity(new UrlEncodedFormEntity(List.of(
            new BasicNameValuePair("chat_id", chatId),
            new BasicNameValuePair("text", getMessage())
        ), StandardCharsets.UTF_8));

        try {
          LOGGER.info("Sending [" + getMessage() + "] to chat_id=[" + chatId + "]");
          LOGGER.info("post result=" + client.execute(post, HttpResponse::getStatusLine));
        } catch (IOException e) {
          result = false;
          LOGGER.warning("Error while sending notification: " + e.getMessage());
        }
      }
    } finally {
      try {
        client.close();
      } catch (IOException ignored) {
      }
    }
    return result;
  }

  private String getMessage() {
    return String.format(
            "%s%n%n%s%n%n%s <%s>%n%n%s",
            title,
            content,
            urlTitle,
            url,
            extraMessage
    );
  }

}
