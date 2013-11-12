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

package org.fao.geonet.geocat.kernel.reusable;

import static org.fao.geonet.geocat.kernel.reusable.Utils.addChild;

import java.sql.SQLException;
import java.util.List;

import jeeves.resources.dbms.Dbms;
import jeeves.utils.SerialFactory;

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.csw.common.util.Xml;
import org.fao.geonet.util.ISODate;
import org.jdom.Element;

/**
 * Handles operations for interacting with the Deleted Objects database table
 * 
 * @author jeichar
 */
public final class DeletedObjects
{

    public static int insert(Dbms dbms, SerialFactory sf, String fragment, String desc) throws SQLException
    {

        int id = sf.getSerial(dbms, "DeletedObjects");

        String date = new ISODate().toString();

        dbms.execute("INSERT INTO DeletedObjects (id,description,xml,deletionDate) VALUES (?,?,?,?)", id, desc,
                fragment, date);
        return id;
    }

    public static String href(int id)
    {
        return "local://xml.reusable.deleted?id=" + id;
    }

    public static Element get(Dbms dbms, String id) throws Exception
    {

        Element records = dbms.select("SELECT xml FROM DeletedObjects where id = ?", new Integer(id).intValue());

        final Element obj = Xml.loadString(records.getChild("record").getChildText("xml"), false);
        obj.removeAttribute("title", Geonet.Namespaces.XLINK);
        obj.removeNamespaceDeclaration(Geonet.Namespaces.XLINK);
        return obj;
    }

    public static Element list(Dbms dbms) throws SQLException
    {

        List<Element> records = dbms.select("SELECT id,description,deletionDate FROM DeletedObjects order by deletionDate").getChildren();

        Element deleted = new Element(ReplacementStrategy.REPORT_ROOT);
        for (Element element : records) {
            Element delete = new Element(ReplacementStrategy.REPORT_ELEMENT);
            String desc = element.getChildTextTrim("description");
            String date = element.getChildTextTrim("deletiondate");
            if (date != null) {
                if (desc == null) {
                    desc = date;
                } else {
                    desc = date + " - " + desc ;
                }
            }
            String id = element.getChildTextTrim("id");
            Utils.addChild(delete, ReplacementStrategy.REPORT_ID, id);
            if (desc != null) {
                Utils.addChild(delete, ReplacementStrategy.REPORT_DESC, desc);
                delete.setAttribute("desc", desc+" - "+date);
            }
            if (date != null) {
                Utils.addChild(delete, "date", date);
            }

            delete.addContent(new Element("xlink").setText(href(Integer.parseInt(id))));
            delete.addContent(new Element("search").setText(id+desc+date));
            delete.addContent(new Element("type").setText("deleted"));
            deleted.addContent(delete);
        }

        return deleted;
    }

    public static void delete(Dbms dbms, String[] ids) throws SQLException
    {
        dbms.execute("DELETE FROM DeletedObjects WHERE " + Utils.constructWhereClause("id", ids));
    }

    public static String[] getLuceneIndexField() {
        return new String[]{"xlink_deleted"};
    }

}
