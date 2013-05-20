//==============================================================================
//===	Copyright (C) 2001-2008 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.services.reusable;

import static org.fao.geonet.kernel.reusable.Utils.addChild;
import static org.fao.geonet.util.LangUtils.iso19139DefaultLang;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import jeeves.interfaces.Service;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.utils.Util;

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.kernel.reusable.*;
import org.fao.geonet.util.GeocatXslUtil;
import org.fao.geonet.util.LangUtils;
import org.jdom.Element;

import scala.actors.threadpool.Arrays;

/**
 * Return the metadata that references the reusable object
 *
 * @author jeichar
 */
public class ReferencingMetadata implements Service
{

    @SuppressWarnings("unchecked")
	public Element exec(Element params, ServiceContext context) throws Exception
    {

        String id = Util.getParam(params, "id");
        String type = Util.getParam(params, "type");
        Dbms dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);

        List<String> fields = new LinkedList<String>();
        Function<String,String> idConverter;
        if (type.equalsIgnoreCase("deleted")) {
            fields.addAll(Arrays.asList(DeletedObjects.getLuceneIndexField()));
            idConverter= ReplacementStrategy.ID_FUNC;
        } else {

            final ReplacementStrategy replacementStrategy = Utils.strategy(ReusableTypes.valueOf(type), context);
            fields.addAll(Arrays.asList(replacementStrategy.getInvalidXlinkLuceneField()));
            fields.addAll(Arrays.asList(replacementStrategy.getValidXlinkLuceneField()));
            idConverter=replacementStrategy.numericIdToConcreteId(context.getUserSession());
        }

        Set<MetadataRecord> md = Utils.getReferencingMetadata(context, fields, id, true,idConverter);
        Element reponse = new Element("reponse");
        for (MetadataRecord metadataRecord : md) {
            
            Element record = new Element("record");
            reponse.addContent(record);

            Utils.addChild(record, "id", metadataRecord.id);

            try {
                Element titleElement = metadataRecord.xml.getChild("identificationInfo", Geonet.Namespaces.GMD).getChild(
                        "CHE_MD_DataIdentification", GeocatXslUtil.CHE_NAMESPACE).getChild("citation", Geonet.Namespaces.GMD).getChild(
                        "CI_Citation", Geonet.Namespaces.GMD).getChild("title", Geonet.Namespaces.GMD);

                String translated = LangUtils.iso19139TranslatedText(titleElement, context.getLanguage(),
                        iso19139DefaultLang(metadataRecord.xml));

                addChild(record, "title", translated);
            } catch (NullPointerException e) {
                // the get title code is brittle so catch null pointer and
                // assume there is no title:

                addChild(record, "title", "n/a");
            }
            Utils.addChild(record, "name", metadataRecord.name(dbms));
            Utils.addChild(record, "email", metadataRecord.email(dbms));
        }

        return reponse;
    }

    public void init(String appPath, ServiceConfig params) throws Exception
    {
    }

}
