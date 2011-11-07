/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.webservices.integration.metadata;

import java.util.LinkedList;
import java.util.List;

import org.jboss.metadata.ejb.jboss.JBossEnterpriseBeanMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.webservices.integration.WebServiceDeclaration;
import org.jboss.webservices.integration.WebServiceDeployment;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.JBossPortComponentMetaData;

/**
 * Builds container independent meta data from EJB3 container meta data. 
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class MetaDataBuilderEJB3 extends AbstractMetaDataBuilderEJB
{
   /**
    * Constructor.
    */
   MetaDataBuilderEJB3()
   {
      super();
   }

   /**
    * @see AbstractMetaDataBuilderEJB#buildEnterpriseBeansMetaData(Deployment, EJBArchiveMetaData)
    *
    * @param dep webservice deployment
    * @param ejbArchiveMD EJB archive meta data
    */
   @Override
   protected void buildEnterpriseBeansMetaData(final Deployment dep, final EJBArchiveMetaData ejbArchiveMD)
   {
      final JBossMetaData jbossMetaData = WSHelper.getRequiredAttachment(dep, JBossMetaData.class);
      final WebServiceDeployment ejb3Deployment = WSHelper.getRequiredAttachment(dep, WebServiceDeployment.class);
      final List<EJBMetaData> wsEjbsMD = new LinkedList<EJBMetaData>();
      final JBossWebservicesMetaData jbossWebservicesMD = WSHelper.getOptionalAttachment(dep, JBossWebservicesMetaData.class);

      for (final WebServiceDeclaration ejbEndpoint : ejb3Deployment.getServiceEndpoints())
      {
         final String ejbName = ejbEndpoint.getComponentName();
         final JBossEnterpriseBeanMetaData jbossEjbMD = jbossMetaData.getEnterpriseBean(ejbName);
         this.buildEnterpriseBeanMetaData(wsEjbsMD, jbossEjbMD, jbossWebservicesMD);
      }

      ejbArchiveMD.setEnterpriseBeans(wsEjbsMD);
   }

   /**
    * Builds JBoss agnostic EJB meta data.
    *
    * @param wsEjbsMD jboss agnostic EJBs meta data
    * @param jbossEjbMD jboss specific EJB meta data
    */
   private void buildEnterpriseBeanMetaData(final List<EJBMetaData> wsEjbsMD, final JBossEnterpriseBeanMetaData jbossEjbMD, final JBossWebservicesMetaData jbossWebservicesMD)
   {
      log.debug("Creating JBoss agnostic EJB3 meta data for session bean: " + jbossEjbMD.getEjbClass());
      final EJBMetaData wsEjbMD = new SLSBMetaData();
      wsEjbMD.setEjbName(jbossEjbMD.getEjbName());
      wsEjbMD.setEjbClass(jbossEjbMD.getEjbClass());

      if (jbossEjbMD.isSession())
      {
         final JBossPortComponentMetaData portComponentMD = getPortComponent(jbossEjbMD.getEjbName(), jbossWebservicesMD);
         if (portComponentMD != null)
         {
            // set port component meta data
            wsEjbMD.setPortComponentName(portComponentMD.getPortComponentName());
            wsEjbMD.setPortComponentURI(portComponentMD.getPortComponentURI());

            // set security meta data
            final EJBSecurityMetaData smd = new EJBSecurityMetaData();
            smd.setAuthMethod(portComponentMD.getAuthMethod());
            smd.setTransportGuarantee(portComponentMD.getTransportGuarantee());
            smd.setSecureWSDLAccess(portComponentMD.getSecureWSDLAccess());
            wsEjbMD.setSecurityMetaData(smd);
         }
      }

      wsEjbsMD.add(wsEjbMD);
   }

}
