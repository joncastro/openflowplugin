/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.impl.statistics.services.dedicated;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.openflowjava.protocol.api.connection.OutboundQueue;
import org.opendaylight.openflowplugin.api.openflow.device.DeviceContext;
import org.opendaylight.openflowplugin.api.openflow.device.RequestContextStack;
import org.opendaylight.openflowplugin.api.openflow.device.Xid;
import org.opendaylight.openflowplugin.api.openflow.device.handlers.MultiMsgCollector;
import org.opendaylight.openflowplugin.impl.common.MultipartRequestInputFactory;
import org.opendaylight.openflowplugin.impl.services.CommonService;
import org.opendaylight.openflowplugin.impl.services.DataCrate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.MultipartType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartReply;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.MultipartRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.OfHeader;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Martin Bobak &lt;mbobak@cisco.com&gt; on 4.4.2015.
 */
public class StatisticsGatheringService extends CommonService {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsGatheringService.class);

    public StatisticsGatheringService(final RequestContextStack requestContextStack, final DeviceContext deviceContext) {

        super(requestContextStack, deviceContext);
    }


    public Future<RpcResult<List<MultipartReply>>> getStatisticsOfType(final MultipartType type) {
        return handleServiceCall(new Function<DataCrate<List<MultipartReply>>, ListenableFuture<RpcResult<Void>>>() {
                                     @Override
                                     public ListenableFuture<RpcResult<Void>> apply(final DataCrate<List<MultipartReply>> data) {
                                         final Xid xid = data.getRequestContext().getXid();
                                         final DeviceContext deviceContext = getDeviceContext();
                                         final MultiMsgCollector multiMsgCollector = deviceContext.getMultiMsgCollector();

                                         multiMsgCollector.registerMultipartXid(xid.getValue());
                                         MultipartRequestInput multipartRequestInput = MultipartRequestInputFactory.
                                                 makeMultipartRequestInput(xid.getValue(),
                                                         getVersion(),
                                                         type);
                                         final OutboundQueue outboundQueue = deviceContext.getPrimaryConnectionContext().getOutboundQueueProvider().getOutboundQueue();
                                         final SettableFuture<RpcResult<Void>> settableFuture = SettableFuture.create();
                                         outboundQueue.commitEntry(xid.getValue(), multipartRequestInput, new FutureCallback<OfHeader>() {
                                             @Override
                                             public void onSuccess(final OfHeader ofHeader) {
                                                 if (ofHeader instanceof MultipartReply) {
                                                     final MultipartReply multipartReply = (MultipartReply) ofHeader;
                                                     settableFuture.set(RpcResultBuilder.<Void>success().build());
                                                     multiMsgCollector.addMultipartMsg(multipartReply);
                                                 } else {
                                                     if (null != ofHeader) {
                                                         LOG.info("Unexpected response type received {}.", ofHeader.getClass());
                                                     } else {
                                                         LOG.info("Unexpected response type received {}.", ofHeader.getClass());
                                                     }
                                                 }

                                             }

                                             @Override
                                             public void onFailure(final Throwable throwable) {
                                                 RpcResultBuilder rpcResultBuilder = RpcResultBuilder.<Void>failed().withError(RpcError.ErrorType.APPLICATION, throwable.getMessage());
                                                 settableFuture.set(rpcResultBuilder.build());
                                             }
                                         });
                                         return settableFuture;
                                     }
                                 }

        );
    }

}
