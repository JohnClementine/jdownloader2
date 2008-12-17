//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class ShareOnlineBiz extends PluginForHost {
    private String captchaCode;
    private String passCode = null;
    private String url;

    public ShareOnlineBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.share-online.biz/service.php");
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    public void login(Account account) throws IOException, PluginException {
        br.postPage("http://www.share-online.biz/login.php", "act=login&location=service.php&dieseid=&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&login=Log+me+in&folder_autologin=1");
        String cookie = br.getCookie("http://www.share-online.biz", "king_passhash");
        if (cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
        br.getPage("http://www.share-online.biz/members.php");
        String expired = br.getRegex(Pattern.compile("<b>Expired\\?</b></td>.*?<td align=\"left\">(.*?)<a", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (expired == null || !expired.trim().equalsIgnoreCase("no")) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public boolean isPremium() throws IOException {
        if (br.getURL() == null || !br.getURL().equalsIgnoreCase("http://www.share-online.biz/members.php")) {
            br.getPage("http://www.share-online.biz/members.php");
        }
        if (br.containsHTML("<b>Premium account</b>")) { return true; }
        return false;
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setValid(false);
            ai.setStatus("No Premium Account!");
            return ai;
        }
        br.getPage("http://www.share-online.biz/members.php");
        String points = br.getRegex(Pattern.compile("<b>Total Points:</b></td>.*?<td align=\"left\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        String expire = br.getRegex(Pattern.compile("<b>Package Expire Date:</b></td>.*?<td align=\"left\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd/MM/yy", null));
        ai.setTrafficLeft(-1);
        ai.setValid(true);
        return ai;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        url = downloadLink.getDownloadURL() + "&setlang=en";
        this.setBrowserExclusive();
        br.setAcceptLanguage("en, en-gb;q=0.8");
        for (int i = 1; i < 3; i++) {
            try {
                Thread.sleep(1000);/*
                                    * Sicherheitspause, sonst gibts 403 Response
                                    */
                String page = br.getPage(url);
                if (page != null && br.getRedirectLocation() == null) {
                    String filename = br.getRegex(Pattern.compile("<b>File.name:</b></td>.*?<td align=left width=150px><div style=.*?><b>(.*?)</b></div></td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                    String sizev = br.getRegex(Pattern.compile("You have requested <font.*?</font>(.*?).</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);

                    if (filename == null || sizev == null) return false;
                    downloadLink.setDownloadSize(Regex.getSize(sizev.trim()));
                    downloadLink.setName(filename.trim());
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        DownloadLink downloadLink = (DownloadLink) parameter;
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        getFileInformation(parameter);
        login(account);
        if (!this.isPremium()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        br.getPage(downloadLink.getDownloadURL());
        Form form = br.getForm(1);
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
            br.submitForm(form);
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }

            /* PassCode war richtig, also Speichern */
            downloadLink.setProperty("pass", passCode);
        }

        url = br.getRegex("loadfilelink\\.decode\\(\"(.*?)\"\\);").getMatch(0);

        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, url, true, 1);
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        url = downloadLink.getDownloadURL() + "&?setlang=en";
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        File captchaFile = this.getLocalCaptchaFile(this);
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://www.share-online.biz/captcha.php"));
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        /* CaptchaCode holen */
        captchaCode = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
        Form form = br.getForm(1);
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }

        /* Überprüfen(Captcha,Password) */
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        if (br.containsHTML("Captcha number error or expired") || br.containsHTML("Unfortunately the password you entered is not correct")) {
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }

        /* Downloadlimit erreicht */
        if (br.containsHTML("max allowed download sessions") | br.containsHTML("this download is too big")) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(3600000l);
            return;
        }

        /* PassCode war richtig, also Speichern */
        downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen, thx @dwd */
        String all = br.getRegex("eval\\(unescape\\(.*?\"\\)\\)\\);").getMatch(-1);
        String dec = br.getRegex("loadfilelink\\.decode\\(\".*?\"\\);").getMatch(-1);
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + dec + "} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        url = Context.toString(result);
        Context.exit();

        /* 15 seks warten */
        sleep(15000, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, url);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
