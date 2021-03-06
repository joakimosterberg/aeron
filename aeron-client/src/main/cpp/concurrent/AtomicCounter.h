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
#ifndef INCLUDED_AERON_CONCURRENT_ATOMIC_COUNTER__
#define INCLUDED_AERON_CONCURRENT_ATOMIC_COUNTER__

#include <cstdint>
#include <memory>

#include <util/Index.h>
#include "AtomicBuffer.h"
#include "CountersManager.h"

namespace aeron { namespace concurrent {

class AtomicCounter
{
public:

    AtomicCounter(AtomicBuffer& buffer, std::int32_t counterId, std::shared_ptr<CountersManager> countersManager) :
        m_buffer(buffer),
        m_counterId(counterId),
        m_countersManager(countersManager),
        m_offset(CountersManager::counterOffset(counterId))
    {
        m_buffer.putInt64(m_offset, 0);
    }

    AtomicCounter(AtomicBuffer& buffer, std::int32_t counterId) :
        m_buffer(buffer),
        m_counterId(counterId),
        m_countersManager(nullptr),
        m_offset(CountersManager::counterOffset(counterId))
    {
    }

    virtual ~AtomicCounter()
    {
        if (nullptr != m_countersManager)
        {
            m_countersManager->free(m_counterId);
        }
    }

    inline std::int32_t id() const
    {
        return m_counterId;
    }

    inline void increment()
    {
        m_buffer.getAndAddInt64(m_offset, 1);
    }

    inline void orderedIncrement()
    {
        m_buffer.addInt64Ordered(m_offset, 1);
    }

    inline void set(std::int64_t value)
    {
        m_buffer.putInt64Atomic(m_offset, value);
    }

    inline void setOrdered(long value)
    {
        m_buffer.putInt64Ordered(m_offset, value);
    }

    inline void addOrdered(long increment)
    {
        m_buffer.addInt64Ordered(m_offset, increment);
    }

    inline std::int64_t get() const
    {
        return m_buffer.getInt64Volatile(m_offset);
    }

private:
    AtomicBuffer m_buffer;
    std::int32_t m_counterId;
    std::shared_ptr<CountersManager> m_countersManager;
    util::index_t m_offset;
};

}}

#endif
