package org.fao.geonet.component;

import jeeves.server.context.ServiceContext;
import org.fao.geonet.component.csw.GetRecordByIdMetadataTransformer;
import org.fao.geonet.csw.common.OutputSchema;
import org.fao.geonet.csw.common.exceptions.CatalogException;
import org.fao.geonet.csw.common.exceptions.NoApplicableCodeEx;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.services.gm03.ISO19139CHEtoGM03;
import org.fao.geonet.utils.Xml;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.output.DOMOutputter;
import org.springframework.stereotype.Component;

/**
 * User: Jesse
 * Date: 11/7/13
 * Time: 3:28 PM
 */
@Component
public class GeocatGetRecordByIdMetadataTransformer implements GetRecordByIdMetadataTransformer {
    @Override
    public boolean isApplicable(ServiceContext context, Element metadata, OutputSchema outputSchema) {
        final String schema = context.getBean(DataManager.class).autodetectSchema(metadata);
        return "iso19139.che".equals(schema) &&
               (outputSchema.equals(OutputSchema.GM03_PROFILE) || outputSchema.equals(OutputSchema.ISO_PROFILE));
    }

    @Override
    public Element apply(ServiceContext context, Element md, OutputSchema outSchema) throws CatalogException {
        if (outSchema.equals(OutputSchema.GM03_PROFILE)) {
            Element elMd = (Element) md.detach();
            // Perform transfo to produce GM03
            try {
                DOMOutputter outputter = new DOMOutputter();

                // We need w3c XML DOM Documents for XSL transform
                org.jdom.Document doc = new org.jdom.Document(elMd);

                org.w3c.dom.Document domIn = outputter.output(doc);
                // PMT GeoCat2 : Backport from old geocat version. GM03_profile should be activated later,
                // For now leaving it commented out.

                ISO19139CHEtoGM03 toGm03 = new ISO19139CHEtoGM03(null,
                        context.getAppPath() + "xsl/conversion/import/ISO19139CHE-to-GM03.xsl");
                org.w3c.dom.Document domOut = toGm03.convert(domIn);
                DOMBuilder builder = new DOMBuilder();
                return builder.build(domOut).getRootElement();

            } catch (Exception e) {
                throw new NoApplicableCodeEx("Error transforming metadata ISO 19139.CHE into GM03_2Record " + e.getMessage());
            }
        } else {
            try {
                // ISO 19139.che are fetched and considered as classic iso19139
                // we then need to convert the MD into real classic iso19139


                String styleSheet = context.getBean(SchemaManager.class).getSchemaDir("iso19139.che") + "/convert/to19139.xsl";
                //String styleSheet =  context.getAppPath() + Geonet.Path.CONV_STYLESHEETS + "/export/xml_iso19139.xsl";

                Document doc = new Document(new Element("MD_Metadata", "gmd", "http://www.isotc211.org/2005/gmd"));
                doc.getRootElement().setContent(md.cloneContent());
                md = doc.getRootElement();
                return Xml.transform(md, styleSheet);
            } catch (Exception e) {
                throw new NoApplicableCodeEx("Error transforming metadata ISO 19139.CHE into ISO 19139 " + e.getMessage());
            }
        }
    }
}
