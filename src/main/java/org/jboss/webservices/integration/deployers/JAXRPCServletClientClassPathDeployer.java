/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.webservices.integration.deployers;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.webservices.integration.util.ASHelper;

/**
 * A deployer for properly setting the classpath for jaxrpc client deployments;
 * it's triggered by the ApplicationClientMetaData which is attached to the unit
 * when a web.xml descriptor file is found in the deployment.
 * This deployer then actually enables classpath modification when a jaxrpc-mapping
 * is specified in the descriptor.
 *
 * @author alessio.soldano@jboss.com
 * @since 03-Feb-2010
 */
public class JAXRPCServletClientClassPathDeployer extends JAXRPCClientClassPathDeployer<WebMetaData>
{
   public JAXRPCServletClientClassPathDeployer()
   {
      super(WebMetaData.class);
   }

   @Override
   protected boolean isClassPathChangeRequired(VFSDeploymentUnit unit)
   {
      WebMetaData webMetaData = ASHelper.getRequiredAttachment(unit, WebMetaData.class);
      return hasJaxRpcMappingReference(webMetaData.getServiceReferences());
   }
}
