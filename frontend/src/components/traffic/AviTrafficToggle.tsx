import { useEffect, useRef, useState } from 'react'
import { getAviTraffic } from '../../api/trafficApi'
import type { AviTrafficStation } from '../../types/traffic'

interface AviTrafficToggleProps {
  onStationsChange: (stations: AviTrafficStation[]) => void
}

export default function AviTrafficToggle({ onStationsChange }: AviTrafficToggleProps) {
  const [enabled, setEnabled] = useState(false)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const requestRef = useRef<AbortController | null>(null)

  useEffect(() => () => requestRef.current?.abort(), [])

  async function handleToggle(nextEnabled: boolean) {
    requestRef.current?.abort()
    setEnabled(nextEnabled)
    setMessage('')

    if (!nextEnabled) {
      setLoading(false)
      onStationsChange([])
      return
    }

    const controller = new AbortController()
    requestRef.current = controller
    setLoading(true)

    try {
      const stations = await getAviTraffic(controller.signal)
      onStationsChange(stations)
      setMessage(stations.length > 0 ? `${stations.length}개 지점을 표시했습니다.` : '표시할 AVI 지점이 없습니다.')
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return
      onStationsChange([])
      setEnabled(false)
      setMessage(error instanceof Error ? error.message : 'AVI 교통량 정보를 불러오지 못했습니다.')
    } finally {
      if (requestRef.current === controller) {
        setLoading(false)
        requestRef.current = null
      }
    }
  }

  return (
    <section className="avi-traffic-toggle" aria-labelledby="avi-toggle-title">
      <div>
        <strong id="avi-toggle-title">AVI 지점 교통량</strong>
        <span>부산 AVI 수집 지점을 지도에 표시합니다.</span>
      </div>
      <label className="toggle-control">
        <input
          type="checkbox"
          checked={enabled}
          onChange={(event) => void handleToggle(event.target.checked)}
        />
        <span>{loading ? '불러오는 중' : enabled ? 'ON' : 'OFF'}</span>
      </label>
      {message && <p className={enabled ? '' : 'error'}>{message}</p>}
    </section>
  )
}
