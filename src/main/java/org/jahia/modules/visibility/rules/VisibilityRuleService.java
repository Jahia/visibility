/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.visibility.rules;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.drools.core.spi.KnowledgeHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.visibility.VisibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.StringReader;
import java.util.Calendar;

/**
 * Service class for performing content visibility related actions in the right-hand side (consequences) of rules.
 *
 * @author Sergiy Shyrkov
 */
public class VisibilityRuleService {

    private static final String INHERIT_FROM_PARENT = "inherit-from-parent";
    private static Logger logger = LoggerFactory.getLogger(VisibilityRuleService.class);
    private static final String RULE_START_AND_END_DATE = "START_AND_END_DATE";
    private static final String RULE_TYPE_EL = "rule-type";
    private static final String VALID_FROM_DATE_EL = "valid-from-date";
    private static final String VALID_TO_DATE_EL = "valid-to-date";

    private VisibilityService visibilityService;

    /**
     * Creates a visibility condition on the node, based on the legacy (Jahia 5.0.x and 6.0.x/6.1.x) rule settings, serialized in XML
     * format.
     *
     * @param nodeFact        the node the visibility settings should be applied to
     * @param ruleSettingsXml the legacy time-based-publishing settings in XML format
     * @param drools          the rule engine helper class
     * @throws RepositoryException in case of an error
     */
    public void importLegacyRuleSettings(final AddedNodeFact nodeFact,
                                         final String ruleSettingsXml, KnowledgeHelper drools) throws RepositoryException {
        String path = nodeFact.getPath();
        if (StringUtils.isEmpty(ruleSettingsXml)) {
            logger.warn(
                    "No rule settings found. Skip importing legacy visibility settings for node {}.",
                    path);
        }

        if (!visibilityService.getConditions().containsKey("jnt:startEndDateCondition")) {
            // we currently only support migration for "start and end date" rules
            logger.warn("Cannot find visibility condition definition of type {}."
                    + " Skip importing legacy settings for node {}", "jnt:startEndDateCondition",
                    path);
            return;
        }

        if (logger.isInfoEnabled()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Importing legacy visibility settings for node {} using value {}",
                        path, ruleSettingsXml);
            } else {
                logger.info("Importing legacy visibility settings for node {}", path);
            }
        }

        Calendar[] dates = parseStartAndEndDates(ruleSettingsXml);
        if (dates != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Adding visibility condition for node {} with"
                        + " start date '{}' and end date '{}'",
                        new Object[]{path, dates[0] != null ? dates[0].getTime() : null,
                                dates[1] != null ? dates[1].getTime() : null});
            }
            try {
                JCRNodeWrapper node = nodeFact.getNode();
                JCRNodeWrapper visibilityNode = node.hasNode(VisibilityService.NODE_NAME) ? node
                        .getNode(VisibilityService.NODE_NAME) : node.addNode(
                        VisibilityService.NODE_NAME, "jnt:conditionalVisibility");
                if (!visibilityNode.hasNode("startEndDateCondition")) {
                    JCRNodeWrapper cond = visibilityNode.addNode("startEndDateCondition", "jnt:startEndDateCondition");
                    if (dates[0] != null) {
                        cond.setProperty("start", dates[0]);
                    }
                    if (dates[1] != null) {
                        cond.setProperty("end", dates[1]);
                    }
                } else {
                    logger.debug("Node {} already contains a visibility condition of type jnt:startEndDateCondition."
                            + " Skip this one", path);
                }

                if (node.hasProperty("j:legacyRuleSettings")) {
                    node.getProperty("j:legacyRuleSettings").remove();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private Calendar[] parseStartAndEndDates(String ruleSettingsXml) {
        Calendar[] dates = null;
        try {
            Document document = new SAXReader().read(new StringReader(ruleSettingsXml));
            Element root = document.getRootElement();
            if (root != null) {
                Element el = root.element(INHERIT_FROM_PARENT);
                if (el != null && !Boolean.valueOf(el.getText())) {
                    // not inherited -> continue
                    el = root.element(RULE_TYPE_EL);
                    if (el != null && RULE_START_AND_END_DATE.equals(el.getText())) {
                        // we found "start and end date" rule
                        Long from = null;
                        Long to = null;
                        el = root.element(VALID_FROM_DATE_EL);
                        if (el != null) {
                            from = Long.parseLong(el.getText());
                        }
                        el = root.element(VALID_TO_DATE_EL);
                        if (el != null) {
                            to = Long.parseLong(el.getText());
                        }

                        Calendar start = null;
                        if (from != null && from != 0) {
                            start = Calendar.getInstance();
                            start.setTimeInMillis(from);
                        }
                        Calendar end = null;
                        if (to != null && to != 0) {
                            end = Calendar.getInstance();
                            end.setTimeInMillis(to);
                        }

                        if (start != null || end != null) {
                            dates = new Calendar[]{start, end};
                        }
                    } else if (el != null) {
                        logger.warn("Unknown visibility type {}. Skipping.", el.getText());
                    }
                }
            }
        } catch (DocumentException e) {
            logger.error("Error reading legcy rule settings: \n" + ruleSettingsXml, e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Parsed visibility dates: {}", dates);
        }

        return dates;
    }

    /**
     * Injects an instance of the {@link VisibilityService}.
     *
     * @param visibilityService an instance of the {@link VisibilityService}
     */
    public void setVisibilityService(VisibilityService visibilityService) {
        this.visibilityService = visibilityService;
    }

}
