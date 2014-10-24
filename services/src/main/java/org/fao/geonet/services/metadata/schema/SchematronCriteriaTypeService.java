package org.fao.geonet.services.metadata.schema;

import com.google.common.collect.Lists;
import jeeves.constants.Jeeves;
import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.server.dispatchers.guiservices.XmlCacheManager;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.SchematronCriteriaGroup;
import org.fao.geonet.domain.SchematronCriteriaType;
import org.fao.geonet.domain.SchematronRequirement;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.repository.SchematronCriteriaGroupRepository;
import org.fao.geonet.repository.specification.SchematronCriteriaGroupSpecs;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.springframework.data.jpa.domain.Specification;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A service for the metadata validation administration UI.
 *
 * Created by Jesse on 2/9/14.
 */
public class SchematronCriteriaTypeService implements Service {

    private static final String EL_TYPE = "type";
    private static final String EL_VALUE = "value";
    private static final String EL_NAME = "name";
    private static final String EL_LABEL = "label";
    private SchematronService schematronService = new SchematronService();

    @Override
    public void init(String appPath, ServiceConfig params) throws Exception {
    }

    @Override
    public Element exec(Element params, ServiceContext context) throws Exception {
        SchemaManager schemaManager = context.getApplicationContext().getBean(SchemaManager.class);
        SchematronCriteriaGroupRepository criteriaGroupRepo = context.getApplicationContext().getBean(SchematronCriteriaGroupRepository.class);
        final XmlCacheManager cacheManager = context.getBean(XmlCacheManager.class);

        Element schematrons = schematronService.exec(params, context);
        Element results = new Element(Jeeves.Elem.RESPONSE);
        Element schemas = new Element("schemas");
        results.addContent(schemas);
        Element requirements = new Element("requirements");
        for (SchematronRequirement requirement : SchematronRequirement.values()) {
            requirements.addContent(new Element("requirement").setText(requirement.name()));
        }
        results.addContent(requirements);
        @SuppressWarnings("unchecked")
        List<Element> schematronElements = Lists.newArrayList(schematrons.getChildren());

        for (Element element : schematronElements) {
            String schemaname = element.getChildText("schemaname");
            Element schemaEl = schemas.getChild(schemaname);
            if (schemaEl == null) {
                schemaEl = new Element(schemaname);

                schemaEl.addContent(new Element("name").setText(schemaname));
                addCriteriaTypeDefinition(context, schemaManager, schemaEl, schemaname);
                schemas.addContent(schemaEl);
            }

            element.setName("schematron");
            addTitleToSchematronElement(context, cacheManager, element, schemaManager.getSchemaDir(schemaname));
            schemaEl.addContent(element.detach());

            addSchematronGroupCount(criteriaGroupRepo, element, element.getChildText("id"));
        }

        return results;
    }

    private void addTitleToSchematronElement(ServiceContext context, XmlCacheManager cacheManager, Element element, String schemaDir) throws JDOMException, IOException {
        final String ruleName = element.getChildText("rulename");
        final Element strings = loadTranslations(context, schemaDir, ruleName + ".xml", cacheManager);
        String title = ruleName;
        if (strings.getChild("schematron.title") != null) {
            title = strings.getChildText("schematron.title");
        }
        element.addContent(new Element("title").setText(title));
    }

    private void addSchematronGroupCount(SchematronCriteriaGroupRepository criteriaGroupRepo, Element schemaEl, String id) {
        final Specification<SchematronCriteriaGroup> spec = SchematronCriteriaGroupSpecs.hasSchematronId(Integer.parseInt(id));
        String count = String.valueOf(criteriaGroupRepo.count(spec));
        schemaEl.addContent(new Element("groupCount").setText(count));
    }

    private void addCriteriaTypeDefinition(ServiceContext context, SchemaManager schemaManager, Element schemaEl, String schemaName) throws IOException, JDOMException {

        final String schemaDir = schemaManager.getSchemaDir(schemaName);
        File file = new File(schemaDir, "schematron" + File.separator + "criteria-type.xml");

        final XmlCacheManager cacheManager = context.getBean(XmlCacheManager.class);
        Element criteriaTypeTranslations = loadTranslations(context, schemaDir, "criteria-type.xml", cacheManager);

        if (file.exists()) {
            Element criteriaType = Xml.loadFile(file);
            criteriaType.setName("criteriaTypes");
            criteriaType.addContent(alwaysAcceptCriteriaType());
            criteriaType.addContent(genericXPathCriteriaType());

            @SuppressWarnings("unchecked") List<Element> types = criteriaType.getChildren();
            for (Element type : types) {
                final String name = type.getChildText(EL_NAME).toUpperCase();
                String label = criteriaTypeTranslations.getChildText(name.toLowerCase());
                if (label == null ) {
                    label = name;
                }
                type.addContent(new Element(EL_LABEL).setText(label));
            }
            schemaEl.addContent(criteriaType);
        }
    }

    private Element loadTranslations(ServiceContext context, String schemaDir, String translationFile, XmlCacheManager cacheManager) throws JDOMException, IOException {
        Element criteriaTypeTranslations;
        try {
            criteriaTypeTranslations = cacheManager.get(context, true, schemaDir + File.separator + "loc",
                    translationFile, context.getLanguage(), Geonet.DEFAULT_LANGUAGE, true, true);
        } catch (FileNotFoundException e) {
            // there is a case where the schematron plugin doesn't have any translations for the criteria (maybe there aren't any criteria).
            criteriaTypeTranslations = new Element("strings");
        }
        return criteriaTypeTranslations;
    }

    private Element genericXPathCriteriaType() {
       return new Element(EL_TYPE).addContent(Arrays.asList(
               new Element(EL_VALUE).setText("@@value@@"),
               new Element(EL_TYPE).setText(SchematronCriteriaType.XPATH.name()),
               new Element(EL_NAME).setText(SchematronCriteriaType.XPATH.name()))
       );
    }

    private Element alwaysAcceptCriteriaType() {
        return new Element(EL_TYPE).addContent(Arrays.asList(
                new Element(EL_TYPE).setText(SchematronCriteriaType.ALWAYS_ACCEPT.name()),
                new Element(EL_NAME).setText(SchematronCriteriaType.ALWAYS_ACCEPT.name()))
        );
    }
}
