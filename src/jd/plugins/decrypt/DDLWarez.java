//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de

//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.

//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.http.GetRequest;
import jd.parser.Form;
import jd.parser.HTMLParser;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class DDLWarez extends PluginForDecrypt {
    private static final String host = "ddl-warez.org";
    private static final String version = "1.0.0.0";
    // http://ddl-warez.org/detail.php?id=5380&cat=games
    private static final Pattern patternSupported = getSupportPattern("http://[*]ddl-warez\\.org/detail\\.php\\?id=[+]&cat=[+]");
    private static final Pattern patternFrame = Pattern.compile("<frame\\s.*?src=\"(.*?)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    public DDLWarez() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("ddl-warez");
    }

    @Override
    public String getCoder() {
        return "Bo0nZ";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                GetRequest req = new GetRequest(parameter);
                req.setReadTimeout(5*60*1000);
                req.setConnectTimeout(5*60*1000);
                String page=req.load();
                //RequestInfo reqinfo = HTTP.getRequest(url); // Seite aufrufen
                String title = SimpleMatches.getSimpleMatch(page, "<title>DDL-Warez v3.0 // °</title>", 0);
                String pass = SimpleMatches.getSimpleMatch(page, "<td>Passwort:</td>°<td style=\"padding-left:10px;\">°</td>°</tr>", 1);
                if (pass.equals("kein Passwort")) pass = null;
                logger.info("Password=" + pass);
               
                Form[] forms =  Form.getForms(page);

                // first form is the search form, not needed
                progress.setRange(forms.length - 1);
                Matcher frameMatcher = null;
                FilePackage fp = new FilePackage();
                fp.setName(title);
                fp.setPassword(pass);

                for (int i = 1; i < forms.length; ++i) {
                    RequestInfo formInfo = forms[i].getRequestInfo();

                    // signed: the 2nd redirection layer was removed
                    // URL urlredirector = new
                    // URL(getBetween(formInfo.getHtmlCode(), "<FRAME SRC=\"",
                    // "\""));
                    // RequestInfo reqinfoRS = getRequest(urlredirector);

                    // System.out.println(formInfo.getHtmlCode());

                    for (Iterator<String> it = SimpleMatches.getAllSimpleMatches(formInfo, this.patternFrame, 1).iterator(); it.hasNext();) {

                        String[] links = HTMLParser.getHttpLinks(it.next().trim(), null);

                        for (String link : links) {
                            try {
                                if (JDUtilities.getPluginForHost(new URL(link).getHost()) != null) {
                                    ;
                                    decryptedLinks.add(this.createDownloadlink(link));
                                    decryptedLinks.lastElement().setFilePackage(fp);
                                    decryptedLinks.lastElement().addSourcePluginPassword(pass);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }

                    // System.out.println("found: "+found);

                    progress.increase(1);
                }

                // Decrypt abschliessen
                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

}
