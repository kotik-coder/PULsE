package pulse.ui;

import static pulse.ui.Messages.getString;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import pulse.io.readers.ReaderManager;

public class Version {

    private long versionDate;
    private String versionLabel;
    private static Version currentVersion = ReaderManager.readVersion();

    public Version(String label, long versionDate) {
        this.versionLabel = label;
        this.versionDate = versionDate;
    }

    public Version checkNewVersion() {

        try {
            var website = new URL("https://kotik-coder.github.io/Version.txt");
            var conn = website.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            long date = conn.getLastModified();

            if (date == 0) {
                System.out.println("No remote version info found");
            }

            var label = IOUtils.toString(website, "UTF-8");

            return Long.compare(date, versionDate) > 0 ? new Version(label, date) : null;

        } catch (IOException e) {
            System.err.println("Could not check for new version");
            e.printStackTrace();
            return null;
        }
    }

    public long getVersionDate() {
        return versionDate;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public String toString() {
        var fmt = DateFormat.getDateInstance(DateFormat.SHORT);
        return getString("TaskControlFrame.SoftwareTitle") + " - " + versionLabel + " (" + fmt.format(new Date(versionDate)) + ")";
    }

    public static Version getCurrentVersion() {
        return currentVersion;
    }

}
