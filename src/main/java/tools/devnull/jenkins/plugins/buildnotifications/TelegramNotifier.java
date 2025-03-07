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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Date;

/**
 * A notifier that uses Telegram to delivery messages
 *
 * @author Ataxexe
 */
public class TelegramNotifier extends BaseNotifier {

  /**
   * @param globalTarget
   * @param successfulTarget
   * @param brokenTarget
   * @param stillBrokenTarget
   * @param fixedTarget
   * @param sendIfSuccess
   * @param extraMessage
   * @see BaseNotifier#BaseNotifier(String, String, String, String, String, boolean, String)
   */
  @DataBoundConstructor
  public TelegramNotifier(String globalTarget,
                          String successfulTarget,
                          String brokenTarget,
                          String stillBrokenTarget,
                          String fixedTarget,
                          boolean sendIfSuccess,
                          String extraMessage) {
    super(globalTarget, successfulTarget, brokenTarget, stillBrokenTarget, fixedTarget, sendIfSuccess, extraMessage);
  }

  @Override
  protected Message createMessage(String target, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    TelegramDescriptor descriptor = (TelegramDescriptor)getDescriptor();
    // if target null send to very global target
    return new TelegramMessage(Secret.toString(descriptor.getBotToken()),
                               null == target ? descriptor.gettChat_id() : target,
                               replaceEnvString(build, getExtraMessage()),
                               descriptor.gettProxy(),
                               descriptor.gettProxyUsr(),
                               Secret.toString(descriptor.gettProxyPwd()));
  }

  /**
   * The descriptor for the TelegramNotifier plugin
   */
  @Extension
  public static class TelegramDescriptor extends BuildStepDescriptor<Publisher> {

    private Secret botToken;
    private String tProxy;
    private String tProxyUsr;
    private Secret tProxyPwd;
    private String tChat_id;

    public TelegramDescriptor() {
      load();
    }

    public Secret getBotToken() {
      return botToken;
    }

    public String gettProxy() {
      return tProxy;
    }

    public Secret gettProxyPwd() {
      return tProxyPwd;
    }

    public String gettProxyUsr() {
      return tProxyUsr;
    }

    public String gettChat_id() {
      return tChat_id;
    }

    public void settChat_id(String tChat_id) {
      this.tChat_id = tChat_id;
    }

    public void setBotToken(Secret botToken) {
      this.botToken = botToken;
    }

    public void settProxyPwd(Secret tProxyPwd) {
      this.tProxyPwd = tProxyPwd;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {

      JSONObject config = json.getJSONObject("telegram");
      req.bindJSON(this, config);
      this.tProxy = config.getString("tProxy");
      this.tProxyUsr = config.getString("tProxyUsr");
      this.tChat_id = config.getString("tChat_id");
      save();
      return true;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Telegram Notification";
    }

    public FormValidation doTestConnection(
      @QueryParameter("botToken") final Secret botToken,
      @QueryParameter("tChat_id") final String tChat_id,
      @QueryParameter("tProxy") final String tProxy,
      @QueryParameter("tProxyUsr") final String tProxyUsr,
      @QueryParameter("tProxyPwd") final Secret tProxyPwd) {

      TelegramMessage telegramMessage = new TelegramMessage(Secret.toString(botToken), tChat_id, "tst message " + new Date(),
                                                            tProxy, tProxyUsr, Secret.toString(tProxyPwd));
      if (telegramMessage.send()) {
        return FormValidation.ok("Success");
      }

      return FormValidation.error("Somthing went wrong");
    }
  }
}
