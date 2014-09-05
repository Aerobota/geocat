//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
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

package org.fao.geonet.services.relations;

import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import org.fao.geonet.Util;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.MetadataRelation;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.repository.MetadataRelationRepository;
import org.fao.geonet.repository.specification.MetadataRelationSpecs;
import org.fao.geonet.services.Utils;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.data.jpa.domain.Specification;

import java.sql.SQLException;
import java.util.*;

//=============================================================================

public class Get implements Service {
	public void init(String appPath, ServiceConfig params) throws Exception {
	}

	// --------------------------------------------------------------------------
	// ---
	// --- Service
	// ---
	// --------------------------------------------------------------------------

	public Element exec(Element params, ServiceContext context)
			throws Exception {
		int id = Integer.parseInt(Utils.getIdentifierFromParameters(params,
                context));
		String relation = Util.getParam(params, "relation", "normal");

		return getRelation(id, relation, context);
	}

	/**
	 * TODO : should we move relation management in DataManager or in a specific
	 * relation management class ?
	 * 
	 * @param id
	 * @param relation
	 * @param context
	 * @return
	 * @throws Exception 
	 */
	public static Element getRelation(int id, String relation, ServiceContext context) throws Exception {
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		DataManager dm = gc.getBean(DataManager.class);
		
		Set<Integer> result = getRelationIds(id, relation, context);
		
				// --- retrieve metadata and return result
		Element response = new Element("response");

		for (Integer mdId : result) {
            boolean forEditing = false, withValidationErrors = false, keepXlinkAttributes = false;
            Element md = dm.getMetadata(context,"" + mdId, forEditing, withValidationErrors, keepXlinkAttributes);

			// --- we could have a race condition so, just perform a simple check
			if (md != null)
				response.addContent(md);
		}

		return response;
	}

	
	/**
	 * Method to query Relation table and get a Set of identifiers of related
	 * metadata
	 * 
	 * @param id
	 * @param relation
	 * @param context
	 * @return
	 * @throws Exception 
	 */
	public static Set<Integer> getRelationIds(int id, String relation, ServiceContext context) throws Exception {
		// --- perform proper queries to retrieve the id set
		if (relation.equals("normal") || relation.equals("full")) {
			return retrieveIds(context, true, "relatedid", id);
		}

		if (relation.equals("reverse") || relation.equals("full")) {
            return retrieveIds(context, false, "id", id);
		}

		return Collections.emptySet();
	}
	
	/**
	 * Run the query and load a Set based on query results.
	 */
	private static Set<Integer> retrieveIds(ServiceContext context, boolean findMetadataId,
			String field, int id) throws SQLException {
        final MetadataRelationRepository relationRepository = context.getBean(MetadataRelationRepository.class);

        Specification<MetadataRelation> spec;
        if (findMetadataId) {
            spec = MetadataRelationSpecs.hasMetadataId(id);
        } else {
            spec = MetadataRelationSpecs.hasRelatedId(id);
        }

        HashSet<Integer> results = new HashSet<Integer>();
        for (MetadataRelation metadataRelation : relationRepository.findAll(spec)) {
            if (findMetadataId) {
                results.add(metadataRelation.getId().getMetadataId());
            } else {
                results.add(metadataRelation.getId().getRelatedId());
            }
        }
        return results;
	}

    // GEOCAT
    /**
     * Looks for all gmd:aggregationInfo elements and returns them in the form
     * of Elements
     *
     * Created for {@link GetRelated}
     *
     * @param md
     * @param fast
     * @param to
     * @param from
     * @param context
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<String> getAggregationInfos(Element md) {
        List<String> res = new LinkedList<String>();

        final Namespace gmd = md.getNamespace("gmd");
        final Namespace gco = md.getNamespace("gco");
        final Namespace che = md.getNamespace("che");
        Element identificationInfo = md.getChild("identificationInfo", gmd);

        if (identificationInfo == null) {
            return res;
        }

        Element che_md_dataIdentification = identificationInfo.getChild(
                "CHE_MD_DataIdentification", che);

        if (che_md_dataIdentification == null) {
            che_md_dataIdentification = identificationInfo.getChild(
                    "CHE_SV_ServiceIdentification", che);
        }

        if (che_md_dataIdentification == null) {
            return res;
        }

        List<Element> aggregationInfos = che_md_dataIdentification.getChildren(
                "aggregationInfo", gmd);

        for (Element e : aggregationInfos) {
            try {
                Element md_AggregateInformation = e.getChild(
                        "MD_AggregateInformation", gmd);
                Element aggregateDataSetIdentifier = md_AggregateInformation
                        .getChild("aggregateDataSetIdentifier", gmd);
                Element identifier = aggregateDataSetIdentifier.getChild(
                        "MD_Identifier", gmd);
                Element asocType = md_AggregateInformation.getChild(
                        "associationType", gmd);
                asocType = asocType.getChild("DS_AssociationTypeCode", gmd);

                Element uuid = identifier.getChild("code", gmd);
                String type = asocType.getAttributeValue("codeListValue");

                String uuid_ = uuid.getChildText("CharacterString", gco);
                if (uuid_ != null) {
                    res.add(type + " " + uuid_);
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return res;
    }
    // END GEOCAT
}