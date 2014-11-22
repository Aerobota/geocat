package org.fao.geonet.geocat.services.thesaurus;

import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import org.fao.geonet.Util;
import org.fao.geonet.kernel.KeywordBean;
import org.fao.geonet.kernel.Thesaurus;
import org.fao.geonet.kernel.ThesaurusManager;
import org.fao.geonet.kernel.search.KeywordsSearcher;
import org.jdom.Element;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Duplicate implements Service {

    @Override
    public void init(Path appPath, ServiceConfig params) throws Exception {
        
    }

    @Override
    public Element exec(Element params, ServiceContext context) throws Exception {

        String uuid = Util.getParam(params, "uuid");
        String thesaurusName = Util.getParam(params, "thesaurus");

        ThesaurusManager thesaurusManager = context.getBean(ThesaurusManager.class);

        Thesaurus thesaurus = thesaurusManager.getThesaurusByName(thesaurusName);
        KeywordsSearcher ks = new KeywordsSearcher(context, thesaurusManager);
        
        KeywordBean bean = ks.searchById(uuid, thesaurusName, "eng", "fre", "ger", "ita");
        if(bean == null) {
            return new Element("uuid").setText(uuid);
        }
        
        bean.setDefaultLang(mapLang(bean.getDefaultLang()));
        HashMap<String, String> values = new HashMap<String,String>(bean.getValues());
        HashMap<String, String> defs = new HashMap<String,String>(bean.getDefinitions());
        
        for (String lang : values.keySet()) {
            bean.removeValue(lang);
        }
        for (String lang : defs.keySet()) {
            bean.removeDefinition(lang);
        }
        
        for (Map.Entry<String, String> entry: values.entrySet()) {
            bean.setValue(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry: defs.entrySet()) {
            bean.setDefinition(entry.getValue(), entry.getKey());
        }

        if(!(bean.getValues().containsValue("en") || bean.getValues().containsValue("eng"))) {
            bean.setValue(bean.getDefaultValue(), "eng");
        }
        
        if(!(bean.getValues().containsValue("it") || bean.getValues().containsValue("ita"))) {
            bean.setValue(bean.getDefaultValue(), "ita");
        }
        
        if(!(bean.getValues().containsValue("fr") || bean.getValues().containsValue("fre") || bean.getValues().containsValue("fra"))) {
            bean.setValue(bean.getDefaultValue(), "fre");
        }
        
        if(!(bean.getValues().containsValue("de") || bean.getValues().containsValue("deu") || bean.getValues().containsValue("ger"))) {
            bean.setValue(bean.getDefaultValue(), "ger");
        }
        
        thesaurus.updateElement(bean, true);
        
        return new Element("uuid").setText(uuid);
    }

    private String mapLang(String lang) {
        if(!("en".equalsIgnoreCase(lang) || "eng".equalsIgnoreCase(lang))) {
            return "eng";
        }
        
        if(!("it".equalsIgnoreCase(lang) || "ita".equalsIgnoreCase(lang))) {
            return "ita";
        }
        
        if(!("fr".equalsIgnoreCase(lang) || "fre".equalsIgnoreCase(lang) || "fra".equalsIgnoreCase(lang))) {
            return "fre";
        }
        
        if(!("de".equalsIgnoreCase(lang) || "deu".equalsIgnoreCase(lang) || "ger".equalsIgnoreCase(lang))) {
            return "ger";
        }

        return lang;
    }

}
