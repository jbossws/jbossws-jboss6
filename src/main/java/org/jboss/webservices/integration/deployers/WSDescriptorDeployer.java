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
package org.jboss.webservices.integration.deployers;

import org.jboss.wsf.spi.metadata.webservices.WebservicesDescriptorParser;
import org.jboss.wsf.spi.metadata.webservices.WebservicesDescriptorProcessor;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * webservices.xml deployer.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSDescriptorDeployer extends AbstractDescriptorDeployer<WebservicesDescriptorProcessor, WebservicesDescriptorParser, WebservicesMetaData>
{
   /**
    * Constructor.
    */
   public WSDescriptorDeployer()
   {
      super(WebservicesMetaData.class);
   }

   /**
    * MC incallback method. It will be invoked each time WebservicesDescriptorProcessor bean will be installed.
    * @param processor the processor
    */
   @Override
   @Deprecated
   public void setProcessor(final WebservicesDescriptorProcessor processor)
   {
      super.setProcessor(processor);
   }
   
   /**
    * MC incallback method. It will be invoked each time WebservicesDescriptorParser bean will be installed.
    * @param parser the parser
    */
   @Override
   public void setParser(final WebservicesDescriptorParser parser)
   {
      super.setParser(parser);
   }
}
