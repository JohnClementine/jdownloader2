package org.jdownloader.extensions.translator;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;

import jd.captcha.translate.CaptchaTranslation;
import jd.controlling.reconnect.pluginsinc.batch.translate.BatchTranslation;
import jd.controlling.reconnect.pluginsinc.extern.translate.ExternTranslation;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.LiveheaderTranslation;
import jd.controlling.reconnect.pluginsinc.upnp.translate.UpnpTranslation;
import jd.nutils.svn.Subversion;
import jd.plugins.AddonPanel;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.swing.exttable.ExtTableTranslation;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.update.standalone.translate.StandaloneUpdaterTranslation;
import org.appwork.update.updateclient.translation.UpdateTranslation;
import org.appwork.utils.Files;
import org.appwork.utils.locale.AWUTranslation;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialog.LoginData;
import org.jdownloader.api.cnl2.translate.ExternInterfaceTranslation;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.translator.gui.TranslatorGui;
import org.jdownloader.gui.translate.GuiTranslation;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate.JdownloaderTranslation;
import org.tmatesoft.svn.core.SVNException;

/**
 * Extensionclass. NOTE: All extensions have to follow the namescheme to end with "Extension" and have to extend AbstractExtension
 * 
 * @author thomas
 * 
 */
public class TranslatorExtension extends AbstractExtension<TranslatorConfig, TranslateInterface> {
    /**
     * Extension GUI
     */
    private TranslatorGui             gui;
    /**
     * List of all available languages
     */
    private ArrayList<TLocale>        translations;
    /**
     * If a translation is loaded, this list contains all it's entries
     */
    private ArrayList<TranslateEntry> translationEntries;
    /**
     * currently loaded Language
     */
    private TLocale                   loaded;

    public static void main(String[] args) {
        ArrayList<File> files = Files.getFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (!pathname.getName().endsWith(".lng")) return false;

                if (pathname.getName().contains("es-castillian")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("zh-hans")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("zh-hant")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("sr-latin")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "__")));

                }
                if (pathname.getName().contains("pt-BR")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "_")));

                }
                if (pathname.getName().contains("bg-incomplete")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "_")));

                }
                if (pathname.getName().contains("pt-PT")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("-", "_")));

                }
                if (pathname.getName().contains("bg_incomplete")) {
                    renameTo(pathname, new File(pathname.getParentFile(), pathname.getName().replace("_", "__")));

                }
                return false;
            }

            private void renameTo(File pathname, File file) {
                System.out.println(pathname + "->" + file);
                if (file.exists()) file.delete();
                pathname.renameTo(file);
            }
        }, new File("c:\\workspace\\JDownloader"));

    }

    public TranslatorExtension() {
        // Name. The translation Extension itself does not need translation. All
        // translators should be able to read english
        setTitle("Translator");
        // get all LanguageIDs
        List<String> ids = TranslationFactory.listAvailableTranslations(JdownloaderTranslation.class, GuiTranslation.class);
        // create a list of TLocale instances
        translations = new ArrayList<TLocale>();

        for (String id : ids) {
            translations.add(new TLocale(id));
        }
        // sort the list.
        Collections.sort(translations, new Comparator<TLocale>() {

            public int compare(TLocale o1, TLocale o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        // unload extensions on exit
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            {
                setHookPriority(Integer.MAX_VALUE);
            }

            @Override
            public void run() {
                if (!getSettings().isRememberLoginsEnabled()) doLogout();
            }
        });
        // init extension GUI
        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                gui = new TranslatorGui(TranslatorExtension.this);
                return null;
            }
        }.getReturnValue();

    }

    /**
     * Has to return the Extension MAIN Icon. This icon will be used,for example, in the settings pane
     */
    @Override
    public ImageIcon getIcon(int size) {
        return NewTheme.I().getIcon("language", size);
    }

    /**
     * Action "onStop". Is called each time the user disables the extension
     */
    @Override
    protected void stop() throws StopException {
        Log.L.finer("Stopped " + getClass().getSimpleName());
    }

    /**
     * Actions "onStart". is called each time the user enables the extension
     */
    @Override
    protected void start() throws StartException {
        Log.L.finer("Started " + getClass().getSimpleName());
    }

    /**
     * 
     * @return {@link #translations}
     */
    public ArrayList<TLocale> getTranslations() {
        return translations;
    }

    /**
     * gets called once as soon as the extension is loaded.
     */
    @Override
    protected void initExtension() throws StartException {
    }

    /**
     * Returns the Settingspanel for this extension. If this extension does not have a configpanel, null can be returned
     */
    @Override
    public ExtensionConfigPanel<?> getConfigPanel() {
        return null;
    }

    /**
     * Should return false of this extension has no configpanel
     */
    @Override
    public boolean hasConfigPanel() {
        return false;
    }

    /**
     * DO NOT USE THIS FUNCTION. it is only used for compatibility reasons
     */
    @Override
    @Deprecated
    public String getConfigID() {
        return null;
    }

    @Override
    public String getAuthor() {
        return "Coalado";
    }

    @Override
    public String getDescription() {
        return "This Extension can be used to edit JDownloader translations. You need a developer account to use this extension";
    }

    /**
     * Returns the gui
     */
    @Override
    public AddonPanel<? extends AbstractExtension<TranslatorConfig, TranslateInterface>> getGUI() {
        return gui;
    }

    /**
     * Loads the given language
     * 
     * @param locale
     */
    public void load(TLocale locale) {
        loaded = locale;
        ArrayList<TranslateEntry> tmp = new ArrayList<TranslateEntry>();

        for (LazyExtension le : ExtensionController.getInstance().getExtensions()) {
            if (le._getExtension().getTranslation() == null) continue;
            load(tmp, locale, (Class<? extends TranslateInterface>) le._getExtension().getTranslation().getClass().getInterfaces()[0]);

        }
        // use Type Hierarchy in IDE to get all interfaces
        // Extension Translations should not be referenced here
        load(tmp, locale, AWUTranslation.class);
        load(tmp, locale, BatchTranslation.class);
        load(tmp, locale, CaptchaTranslation.class);
        load(tmp, locale, ExternInterfaceTranslation.class);
        load(tmp, locale, ExternTranslation.class);
        load(tmp, locale, ExtTableTranslation.class);
        load(tmp, locale, GuiTranslation.class);
        load(tmp, locale, JdownloaderTranslation.class);
        load(tmp, locale, LiveheaderTranslation.class);
        load(tmp, locale, StandaloneUpdaterTranslation.class);
        load(tmp, locale, UpdateTranslation.class);
        load(tmp, locale, UpnpTranslation.class);

        this.translationEntries = tmp;
    }

    private void load(ArrayList<TranslateEntry> tmp, TLocale locale, Class<? extends TranslateInterface> class1) {
        Log.L.info("Load Translation " + locale + " " + class1);
        TranslateInterface t = TranslationFactory.create(class1, locale.getId());
        for (Method m : t._getHandler().getMethods()) {
            tmp.add(new TranslateEntry(t, m));
        }

    }

    /**
     * 
     * @return {@link #translationEntries}
     */
    public ArrayList<TranslateEntry> getTranslationEntries() {
        return translationEntries;
    }

    /**
     * 
     * @return {@link #loaded}
     */
    public TLocale getLoadedLocale() {
        return loaded;
    }

    private boolean loggedIn = false;

    public boolean isLoggedIn() {
        return loggedIn;
    }

    private void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void doLogin() {
        if (isLoggedIn()) return;

        requestSvnLogin();
    }

    public void doLogout() {
        getSettings().setSVNUser(null);
        getSettings().setSVNPassword(null);
        getSettings().setRememberLoginsEnabled(false);

    }

    public boolean validateSvnLogin(String svnUser, String svnPass) {
        setLoggedIn(false);
        if (svnUser.length() > 3 && svnPass.length() > 3) {
            String url = "svn://svn.jdownloader.org/jdownloader";

            Subversion s = null;
            try {
                s = new Subversion(url, svnUser, svnPass);
                setLoggedIn(true);
                return true;
            } catch (SVNException e) {
                Dialog.getInstance().showMessageDialog("SVN Test Error", "Login failed. Username and/or password are not correct!\r\n\r\nServer: " + url);
            } finally {
                try {
                    s.dispose();
                } catch (final Throwable e) {
                }
            }
        } else {
            Dialog.getInstance().showMessageDialog("SVN Test Error", "Username and/or password seem malformed. Test failed.");
        }
        return false;

    }

    public void requestSvnLogin() {

        while (true) {

            final LoginDialog d = new LoginDialog(0, "Translation Server Login", "To modify existing translations, or to create a new one, you need a JDownloader Translator Account.", null);
            d.setUsernameDefault(getSettings().getSVNUser());
            d.setPasswordDefault(getSettings().getSVNPassword());
            d.setRememberDefault(getSettings().isRememberLoginsEnabled());

            LoginData response;
            try {
                response = Dialog.getInstance().showDialog(d);
            } catch (DialogClosedException e) {
                // if (!this.svnLoginOK) validateSvnLogin();
                return;
            } catch (DialogCanceledException e) {
                // if (!this.svnLoginOK) validateSvnLogin();
                return;
            }
            if (validateSvnLogin(response.getUsername(), response.getPassword())) {
                getSettings().setSVNUser(response.getUsername());
                getSettings().setSVNPassword(response.getPassword());
                getSettings().setRememberLoginsEnabled(response.isSave());

                return;
            }
        }

    }

}
