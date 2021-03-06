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

import java.util.ResourceBundle;

import org.jboss.deployers.vfs.spi.deployer.AbstractVFSParsingDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VirtualFile;
import org.jboss.ws.api.util.BundleUtils;
import org.jboss.wsf.spi.metadata.DescriptorParser;

/**
 * Abstract descriptor deployer which deploys only if particular DD processor is available.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractDescriptorDeployer<P extends DescriptorParser<T>, T> extends AbstractVFSParsingDeployer<T>
{
   private static final ResourceBundle bundle = BundleUtils.getBundle(AbstractDescriptorDeployer.class);
   private P ddParser;

   AbstractDescriptorDeployer(Class<T> output)
   {
      super(output);
      this.setName("UNSPECIFIED");
   }

   @SuppressWarnings({"deprecation", "unchecked"})
   @Override
   protected T parse(final VFSDeploymentUnit unit, final VirtualFile file, final T root) throws Exception
   {
      if (this.ddParser != null)
      {
         return this.ddParser.parse(file.toURL());
      }

      return null;
   }

   /**
    * MC incallback method. It will be invoked each time subclass specific DescriptorParser bean will be installed.
    */
   protected void setParser(final P ddParser)
   {
      if (this.ddParser != null)
         throw new IllegalStateException(BundleUtils.getMessage(bundle, "ONLY_ONE_INSTANCE_CAN_BE_INSTALLED_IN_MC",  this.ddParser.getClass() ));

      this.ddParser = ddParser;
      this.setName(this.ddParser.getDescriptorName());
   }

}
