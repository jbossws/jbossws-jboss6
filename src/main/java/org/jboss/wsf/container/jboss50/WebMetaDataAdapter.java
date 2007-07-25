/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.wsf.container.jboss50;

// $Id$

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployment.J2eeApplicationMetaData;
import org.jboss.deployment.J2eeModuleMetaData;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.WebSecurityMetaData;
import org.jboss.metadata.WebSecurityMetaData.WebResourceCollection;
import org.jboss.metadata.web.Servlet;
import org.jboss.metadata.web.ServletMapping;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebMetaData.PublishLocationAdapter;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedWebSecurityMetaData.UnifiedWebResourceCollection;

import java.util.*;

/**
 * Build container independent web meta data
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class WebMetaDataAdapter
{
   public UnifiedWebMetaData buildUnifiedWebMetaData(Deployment dep, UnifiedDeploymentInfo udi, DeploymentUnit unit)
   {
      String contextRoot = null;
      
      WebMetaData wmd = unit.getAttachment(WebMetaData.class);
      dep.getContext().addAttachment(WebMetaData.class, wmd);

      if (unit.getParent() != null)
      {
         J2eeApplicationMetaData appmd = unit.getParent().getAttachment(J2eeApplicationMetaData.class);
         if (appmd != null)
         {
            J2eeModuleMetaData module = appmd.getModule(udi.getSimpleName());
            if (module != null)
               contextRoot = module.getWebContext();
         }
      }
      
      if (contextRoot == null)
         contextRoot = wmd.getContextRoot();
      
      UnifiedWebMetaData umd = new UnifiedWebMetaData();
      umd.setContextRoot(contextRoot);
      umd.setServletMappings(getServletMappings(wmd));
      umd.setServletClassNames(getServletClassMap(wmd));
      umd.setConfigName(wmd.getConfigName());
      umd.setConfigFile(wmd.getConfigFile());
      umd.setSecurityDomain(wmd.getSecurityDomain());
      umd.setPublishLocationAdapter(getPublishLocationAdpater(wmd));
      umd.setSecurityMetaData(getSecurityMetaData(wmd.getSecurityContraints()));

      dep.getContext().addAttachment(UnifiedWebMetaData.class, umd);
      return umd;
   }

   private PublishLocationAdapter getPublishLocationAdpater(final WebMetaData wmd)
   {
      return new PublishLocationAdapter()
      {
         public String getWsdlPublishLocationByName(String name)
         {
            return wmd.getWsdlPublishLocationByName(name);
         }
      };
   }

   protected List<UnifiedWebSecurityMetaData> getSecurityMetaData(final Iterator securityConstraints)
   {
      ArrayList<UnifiedWebSecurityMetaData> unifiedsecurityMetaData = new ArrayList<UnifiedWebSecurityMetaData>();

      while (securityConstraints.hasNext())
      {
         WebSecurityMetaData securityMetaData = (WebSecurityMetaData)securityConstraints.next();

         UnifiedWebSecurityMetaData current = new UnifiedWebSecurityMetaData();
         unifiedsecurityMetaData.add(current);

         current.setTransportGuarantee(securityMetaData.getTransportGuarantee());

         Map<String, WebResourceCollection> resources = securityMetaData.getWebResources();
         for (WebResourceCollection webResource : resources.values())
         {
            UnifiedWebResourceCollection currentResource = current.addWebResource(webResource.getName());
            for (String currentPattern : webResource.getUrlPatterns())
            {
               currentResource.addPattern(currentPattern);
            }
         }
      }

      return unifiedsecurityMetaData;
   }

   private Map<String, String> getServletMappings(WebMetaData wmd)
   {
      Map<String, String> mappings = new HashMap<String, String>();
      Iterator it = wmd.getServletMappings().iterator();
      while (it.hasNext())
      {
         ServletMapping sm = (ServletMapping)it.next();
         // FIXME - Add support for multiple mappings
         mappings.put(sm.getName(), sm.getUrlPatterns().get(0));
      }
      return mappings;
   }

   private Map<String, String> getServletClassMap(WebMetaData wmd)
   {
      Map<String, String> mappings = new HashMap<String, String>();
      Iterator it = wmd.getServlets().iterator();
      while (it.hasNext())
      {
         Servlet servlet = (Servlet)it.next();
         // Skip JSPs
         if (servlet.getServletClass() == null || servlet.getServletClass().length() == 0)
            continue;

         mappings.put(servlet.getName(), servlet.getServletClass());
      }
      return mappings;
   }
}
