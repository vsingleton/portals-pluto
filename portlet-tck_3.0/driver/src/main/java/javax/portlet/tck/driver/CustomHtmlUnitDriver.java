package javax.portlet.tck.driver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.io.IOException;

public class CustomHtmlUnitDriver extends HtmlUnitDriver {

    /* package-private */ CustomHtmlUnitDriver(BrowserVersion browserVersion, boolean enableJavascript) {
        super(browserVersion, enableJavascript);
    }

    @Override
    protected WebClient modifyWebClient(WebClient initialWebClient) {

        WebClient webClient = super.modifyWebClient(initialWebClient);

        // Don't throw exceptions when JavaScript Errors occur.
        webClient.getOptions().setThrowExceptionOnScriptError(false);

        // Uncomment to filter CSS errors.
        // if (logLevel.intValue() > Level.FINEST.intValue()) {
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        // }

        return webClient;
    }

    public void loadImages() {

        HtmlPage htmlPage = (HtmlPage) lastPage();
        DomNodeList<DomElement> imageElements =
            htmlPage.getElementsByTagName("img");

        for (DomElement imageElement : imageElements) {

            HtmlImage htmlImage = (HtmlImage) imageElement;

            try {

                // Download the image.
                htmlImage.getImageReader();
            }
            catch (IOException e) {
                // do nothing.
            }
        }
    }
}

