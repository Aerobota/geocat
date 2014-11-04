package iso19139che;

import com.google.common.collect.Lists;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.languages.IsoLanguagesMapper;
import org.fao.geonet.repository.IsoLanguageRepository;
import org.fao.geonet.services.metadata.format.AbstractFormatterTest;
import org.fao.geonet.services.metadata.format.FormatterParams;
import org.fao.geonet.services.metadata.format.groovy.Environment;
import org.fao.geonet.services.metadata.format.groovy.EnvironmentImpl;
import org.fao.geonet.services.metadata.format.groovy.Functions;
import org.fao.geonet.utils.Xml;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.util.List;

/**
 * @author Jesse on 10/17/2014.
 */
public class XMLViewFormatterTest extends AbstractFormatterTest {

    @Autowired
    private IsoLanguagesMapper mapper;
    @Autowired
    private IsoLanguageRepository langRepo;
    @Autowired
    private SchemaManager schemaManager;
    @Test
    public void testBasicFormat() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("html", "true");

        final String formatterId = "xml_view";
        FormatterParams fparams = getFormatterFormatterParamsPair
                (request, formatterId).two();
        Environment env = new EnvironmentImpl(fparams, mapper);
        final Functions functions = new Functions(fparams, env, langRepo, schemaManager);

//        measureFormatterPerformance(request, formatterId);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        formatService.exec("eng", "html", "" + id, null, formatterId, "true", false, request, response);
        final String view = response.getContentAsString();
        //Files.write(view, new File("/home/fgravin/dev/swisstopo/geocat/web-ui/src/main/resources/catalog/tmp/view.html"), Constants.CHARSET);

        List<String> excludes = Lists.newArrayList(
                "> gmd:MD_Metadata > gmd:identificationInfo > gmd:MD_DataIdentification > gmd:citation > gmd:CI_Citation > gmd:title > " +
                "gco:PT_FreeText > gco:textGroup > gmd:LocalisedCharacterString > Text"
        );

        final Element xmlEl = Xml.loadString(xml, false);
        final List text = Lists.newArrayList(Xml.selectNodes(xmlEl, "*//node()[not(@codeList)]/text()"));
        text.addAll(Lists.newArrayList(Xml.selectNodes(xmlEl, "*//node()[@codeList]/@codeListValue")));

        StringBuilder missingStrings = new StringBuilder();
        for (Object t : text) {
            final String path;
            final String requiredText;
            if (t instanceof Text) {
                requiredText = ((Text) t).getTextTrim();
                path = getXPath((Content) t).trim();
            } else if (t instanceof Attribute) {
                Attribute attribute = (Attribute) t;
                final String codelist = attribute.getParent().getAttributeValue("codeList");
                final String code = attribute.getValue();
                requiredText = functions.codelistValueLabel(codelist, code);
                path = getXPath(attribute.getParent()).trim() + "> @codeListValue";
            } else {
                throw new AssertionError(t.getClass() + " is not handled");
            }
            if (!requiredText.isEmpty() && !view.contains(requiredText)) {
                if (!excludes.contains(path)) {
                    missingStrings.append("\n").append(path).append(" -> ").append(requiredText);
                }
            }
        }

        if (missingStrings.length() > 0) {
            throw new AssertionError("The following text elements are missing from the view:" + missingStrings);
        }
    }

    private String getXPath(Content el) {
        String path = "";
        if (el.getParentElement() != null) {
            path = getXPath(el.getParentElement());
        }
        if (el instanceof Element) {
            return path + " > " + ((Element) el).getQualifiedName();
        } else {
            return path + " > " + el.getClass().getSimpleName();
        }
    }

    @Override
    protected File getTestMetadataFile() {
        final String mdFile = XMLViewFormatterTest.class.getResource("/iso19139che/example.xml").getFile();
        return new File(mdFile);
    }
}
