package org.fao.geonet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.kernel.GeonetworkDataDirectory;
import org.fao.geonet.kernel.KeywordBean;
import org.fao.geonet.languages.IsoLanguagesMapper;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import v300.RemoveDuplicateLocalizedTextEl;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.jdom.Namespace.XML_NAMESPACE;

/**
 * @author Jesse on 4/7/2015.
 */
public class RepairRdfFiles {
    private static final List<Namespace> NSs = Lists.newArrayList();
    public static final String RDF_NAMESPACE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final Namespace RDF_NAMESPACE = Namespace.getNamespace("rdf", RDF_NAMESPACE_URI);
    private static final Namespace SKOS_NAMESPACE = Namespace.getNamespace("skos", "http://www.w3.org/2004/02/skos/core#");
    private static final IsoLanguagesMapper langMapper = new IsoLanguagesMapper(){
        @Override
        public String iso639_1_to_iso639_2(String iso639_1) {
            return iso639_1;
        }

        @Override
        public String iso639_2_to_iso639_1(String iso639_2) {
            return iso639_2;
        }

        @Override
        public String iso639_1_to_iso639_2(String iso639_1, String defaultLang) {
            return iso639_1;
        }

        @Override
        public String iso639_2_to_iso639_1(String iso639_2, String defaultLang) {
            return iso639_2;
        }
    };

    public void repair(GeonetworkDataDirectory dataDirectory) throws SQLException {
        final Path localThesauri = dataDirectory.getThesauriDir().resolve("local");

        try {
            Files.walkFileTree(localThesauri, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith(".rdf")) {
                        updateRDF(file);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateRDF(Path thesaurusFile) {
        if (!RemoveDuplicateLocalizedTextEl.UPGRADE_RAN) {
            return;
        }

        final Element element;
        try {
            element = Xml.loadFile(thesaurusFile);

            final List<Element> children = Lists.newArrayList(element.getChildren());
            Map<String, KeywordBean> beans = Maps.newHashMap();

            for (Element child : children) {
                if (child.getAttribute("nodeID", RDF_NAMESPACE) != null) {
                    child.detach();
                } else if (child.getName().equals("Description") &&
                    child.getNamespaceURI().equals(RDF_NAMESPACE_URI) &&
                    child.getAttributeValue("about", RDF_NAMESPACE) != null &&
                    child.getAttributeValue("about", RDF_NAMESPACE).contains("#")) {

                    child.detach();
                    final String id = child.getAttributeValue("about", RDF_NAMESPACE);
                    KeywordBean keywordBean = beans.get(id);
                    if (keywordBean == null) {
                        keywordBean = new KeywordBean(langMapper);

                        keywordBean.setUriCode(id);
                        beans.put(id, keywordBean);
                    }

                    List<Element> prefLabels = child.getChildren("prefLabel", SKOS_NAMESPACE);

                    for (Element prefLabel : prefLabels) {
                        String lang = prefLabel.getAttributeValue("lang", XML_NAMESPACE);

                        String value = prefLabel.getTextTrim();
                        if (value != null && !value.isEmpty()) {
                            if (lang == null) {
                                Log.error(Geonet.GEONETWORK,
                                        "No language defined in the RDF file " + thesaurusFile + " for the keyword with id: " +
                                        id + " and the prefLabel: " + value);
                            }

                            keywordBean.setValue(value, lang);
                        }
                    }

                    List<Element> notes = child.getChildren("scopeNote", SKOS_NAMESPACE);
                    for (Element note : notes) {
                        String lang = note.getAttributeValue("lang", XML_NAMESPACE);
                        String value = note.getTextTrim();
                        if (value != null && !value.isEmpty()) {
                            if (lang == null) {
                                Log.error(Geonet.GEONETWORK, "No language defined in the RDF file " + thesaurusFile + " for the keyword with id: " + id + " and the scopeNote: " + value);
                            }

                            keywordBean.setDefinition(value, lang);
                        }
                    }
                }
            }

            for (KeywordBean keywordBean : beans.values()) {
                final Element desc = new Element("Description", RDF_NAMESPACE).
                        setAttribute("about", keywordBean.getUriCode(), RDF_NAMESPACE).
                        addContent(new Element("type", RDF_NAMESPACE).
                                setAttribute("resource", "http://www.w3.org/2004/02/skos/core#Concept", RDF_NAMESPACE));

                for (Map.Entry<String, String> entry : keywordBean.getValues().entrySet()) {
                    desc.addContent(
                            new Element("prefLabel", SKOS_NAMESPACE).
                                    setAttribute("lang", entry.getKey(), XML_NAMESPACE).
                                    setText(entry.getValue())
                    );
                }

                for (Map.Entry<String, String> entry : keywordBean.getDefinitions().entrySet()) {
                    desc.addContent(
                            new Element("scopeNote", SKOS_NAMESPACE).
                                    setAttribute("lang", entry.getKey(), XML_NAMESPACE).
                                    setText(entry.getValue())
                    );
                }

                element.addContent(desc);
            }

            Files.write(thesaurusFile, Xml.getString(element).getBytes(Constants.CHARSET));
        } catch (JDOMException | IOException e) {
            throw new RuntimeException(e);
        }

    }
}
