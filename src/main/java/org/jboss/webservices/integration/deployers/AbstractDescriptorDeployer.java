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

import org.jboss.deployers.vfs.spi.deployer.ObjectModelFactoryDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.vfs.VFSInputSource;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.metadata.DescriptorProcessor;
import org.jboss.xb.binding.ObjectModelFactory;
import org.xml.sax.InputSource;

/**
 * Abstract descriptor deployer.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractDescriptorDeployer<P extends DescriptorProcessor<T>, T> extends ObjectModelFactoryDeployer<T>
{
   private P ddProcessor;

   AbstractDescriptorDeployer(Class<T> output)
   {
      super(output);
      this.setName("UNSPECIFIED");
      this.setUseSchemaValidation(false);
   }

   @Override
   protected T parse(final VFSDeploymentUnit unit, final VirtualFile file, final T root) throws Exception
   {
      if (this.ddProcessor != null)
      {
         InputSource source = new VFSInputSource(file);
         ObjectModelFactory factory = this.ddProcessor.getFactory(file.toURL());
         return getHelper().parse(source, root, factory);
      }
      
      return null;
   }

   /**
    * MC incallback method. It will be invoked each time subclass specific DescriptorProcessor bean will be installed.
    */
   protected void setProcessor(final P ddProcessor)
   {
      if (this.ddProcessor != null)
         throw new IllegalStateException("Only one " + this.ddProcessor.getClass() + " instance can be installed in MC");
      
      this.ddProcessor = ddProcessor;
      this.setName(this.ddProcessor.getDescriptorName());
      this.setUseSchemaValidation(this.ddProcessor.isValidating());
   }
   
   @Override
   protected ObjectModelFactory getObjectModelFactory(final T root)
   {
      throw new UnsupportedOperationException();
   }
}
