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

//$Id$

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.virtual.VirtualFile;
import org.jboss.wsf.common.DOMUtils;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;
import org.jboss.wsf.spi.metadata.webservices.WebservicesFactory;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.xb.binding.ObjectModelFactory;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.w3c.dom.Element;

import java.net.URL;

/**
 * An abstract web service deployer.
 * 
 *    deploy(unit) 
 *      if(isWebServiceDeployment)
 *        dep = createDeployment(unit)
 *        deploy(dep)
 *
 *    undeploy(unit)
 *      dep = getDeployment(unit) 
 *      undeploy(dep)
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public abstract class ArchiveDeployerHook extends AbstractDeployerHook
{

   /** Depending on the type of deployment, this method should return true
    *  if the deployment contains web service endpoints.
    */
   public abstract boolean isWebServiceDeployment(DeploymentUnit unit);

   /** Create the Deployment for a given DeploymentUnit
    */
   public abstract Deployment createDeployment(DeploymentUnit unit);

   /** Get the Deployment for a given DeploymentUnit
    */
   public Deployment getDeployment(DeploymentUnit unit)
   {
      Deployment dep = unit.getAttachment(Deployment.class);
      return (dep != null && dep.getType() == getDeploymentType() ? dep : null);
   }

   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (ignoreDeployment(unit))
         return;

      if (isWebServiceDeployment(unit))
      {
         log.debug("deploy: " + unit.getName());
         Deployment dep = getDeployment(unit);
         if (dep == null)
         {
            dep = createDeployment(unit);
            dep.addAttachment(DeploymentUnit.class, unit);
         }

         getDeploymentAspectManager().deploy(dep);
         unit.addAttachment(Deployment.class, dep);
      }
   }

   public void undeploy(DeploymentUnit unit)
   {
      if (ignoreDeployment(unit))
         return;

      Deployment dep = getDeployment(unit);
      if (dep != null)
      {
         log.debug("undeploy: " + unit.getName());
         getDeploymentAspectManager().undeploy(dep);
         unit.removeAttachment(Deployment.class);
      }
   }

   /** Unmrashall the webservices.xml if there is one
    */
   protected WebservicesMetaData getWebservicesMetaData(DeploymentUnit unit)
   {
      WebservicesMetaData wsMetaData = unit.getAttachment(WebservicesMetaData.class);
      UnifiedVirtualFile vfWebservices = getWebservicesFile(unit);
      if (wsMetaData == null && vfWebservices != null)
      {
         try
         {
            URL wsURL = vfWebservices.toURL();
            Element root = DOMUtils.parse(wsURL.openStream());
            String namespaceURI = root.getNamespaceURI();
            if (namespaceURI.equals("http://java.sun.com/xml/ns/j2ee"))
            {
               Unmarshaller unmarshaller = UnmarshallerFactory.newInstance().newUnmarshaller();
               ObjectModelFactory factory = new WebservicesFactory(wsURL);
               wsMetaData = (WebservicesMetaData)unmarshaller.unmarshal(wsURL.openStream(), factory, null);
               unit.addAttachment(WebservicesMetaData.class, wsMetaData);
            }
         }
         catch (Exception ex)
         {
            throw new WSFDeploymentException(ex);
         }
      }
      return wsMetaData;
   }

   private UnifiedVirtualFile getWebservicesFile(DeploymentUnit unit)
   {
      VirtualFile vf = ((VFSDeploymentUnit)unit).getMetaDataFile("webservices.xml");
      return (vf != null ? new VirtualFileAdaptor(vf) : null);
   }
}
