/*
 * Copyright 2014-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.archive;

import io.aeron.Image;
import io.aeron.ImageFragmentAssembler;
import io.aeron.archive.codecs.SourceLocation;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.collections.Long2ObjectHashMap;

class ControlSessionDemuxer implements Session, ControlRequestListener
{
    enum State
    {
        ACTIVE, INACTIVE, CLOSED
    }

    private static final int FRAGMENT_LIMIT = 16;

    private final Image image;
    private final ArchiveConductor conductor;
    private final FragmentHandler adapter = new ImageFragmentAssembler(new ControlRequestAdapter(this));
    private final Long2ObjectHashMap<ControlSession> controlSessionByIdMap = new Long2ObjectHashMap<>();

    private State state = State.ACTIVE;

    ControlSessionDemuxer(final Image image, final ArchiveConductor conductor)
    {
        this.image = image;
        this.conductor = conductor;
    }

    public long sessionId()
    {
        return image.correlationId();
    }

    public void abort()
    {
        state = State.INACTIVE;
    }

    public void close()
    {
        state = State.CLOSED;
    }

    public boolean isDone()
    {
        return state == State.INACTIVE;
    }

    public int doWork()
    {
        int workCount = 0;

        if (state == State.ACTIVE)
        {
            if (image.isClosed())
            {
                state = State.INACTIVE;
                for (final Session session : controlSessionByIdMap.values())
                {
                    session.abort();
                }
            }
            else
            {
                workCount += image.poll(adapter, FRAGMENT_LIMIT);
            }
        }

        return workCount;
    }

    public void onConnect(final long correlationId, final String channel, final int streamId)
    {
        final ControlSession session = conductor.newControlSession(correlationId, streamId, channel, this);
        controlSessionByIdMap.put(session.sessionId(), session);
    }

    public void onCloseSession(final long controlSessionId)
    {
        final ControlSession session = controlSessionByIdMap.get(controlSessionId);
        if (null != session)
        {
            session.abort();
        }
    }

    public void onStartRecording(
        final long controlSessionId,
        final long correlationId,
        final int streamId,
        final String channel,
        final SourceLocation sourceLocation)
    {
        final ControlSession controlSession = controlSessionByIdMap.get(controlSessionId);
        if (controlSession == null)
        {
            throw new IllegalArgumentException("Unknown controlSessionId: " + controlSessionId);
        }

        controlSession.onStartRecording(correlationId, channel, streamId, sourceLocation);
    }

    public void onStopRecording(
        final long controlSessionId,
        final long correlationId,
        final int streamId,
        final String channel)
    {
        final ControlSession controlSession = controlSessionByIdMap.get(controlSessionId);
        if (controlSession == null)
        {
            throw new IllegalArgumentException("Unknown controlSessionId: " + controlSessionId);
        }

        controlSession.onStopRecording(correlationId, streamId, channel);
    }

    public void onStartReplay(
        final long controlSessionId,
        final long correlationId,
        final long recordingId,
        final long position,
        final long length,
        final int replayStreamId,
        final String replayChannel)
    {
        final ControlSession controlSession = controlSessionByIdMap.get(controlSessionId);
        if (controlSession == null)
        {
            throw new IllegalArgumentException("Unknown controlSessionId: " + controlSessionId);
        }

        controlSession.onStartReplay(correlationId, recordingId, position, length, replayStreamId, replayChannel);
    }

    public void onStopReplay(final long controlSessionId, final long correlationId, final long replaySessionId)
    {
        final ControlSession controlSession = controlSessionByIdMap.get(controlSessionId);
        if (controlSession == null)
        {
            throw new IllegalArgumentException("Unknown controlSessionId: " + controlSessionId);
        }

        controlSession.onStopReplay(correlationId, replaySessionId);
    }

    public void onListRecordingsForUri(
        final long controlSessionId,
        final long correlationId,
        final long fromRecordingId,
        final int recordCount,
        final int streamId,
        final String channel)
    {
        final ControlSession controlSession = controlSessionByIdMap.get(controlSessionId);
        if (controlSession == null)
        {
            throw new IllegalArgumentException("Unknown controlSessionId: " + controlSessionId);
        }

        controlSession.onListRecordingsForUri(correlationId, fromRecordingId, recordCount, streamId, channel);
    }

    public void onListRecordings(
        final long controlSessionId,
        final long correlationId,
        final long fromRecordingId,
        final int recordCount)
    {
        final ControlSession controlSession = controlSessionByIdMap.get(controlSessionId);
        if (controlSession == null)
        {
            throw new IllegalArgumentException("Unknown controlSessionId: " + controlSessionId);
        }

        controlSession.onListRecordings(correlationId, fromRecordingId, recordCount);
    }

    void removeControlSession(final ControlSession controlSession)
    {
        controlSessionByIdMap.remove(controlSession.sessionId());
    }
}
