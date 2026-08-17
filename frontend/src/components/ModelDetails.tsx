import type { ChatModel, EmbeddingModel } from '../api/types'
import {
  availabilityTone,
  embeddingQueryMayLeaveLocalMachine,
  formatModelAvailability,
  formatModelMode,
} from '../utils/modelFormatting'

type Detail = { label: string; value: string; availability?: string }

export function EmbeddingModelDetails({
  model,
}: {
  model: EmbeddingModel | undefined
}) {
  if (!model) {
    return (
      <ModelDetailsRenderer
        className="embedding-model-details"
        emptyMessage="Embedding model availability, dimensions, mode, and privacy appear here after selection."
      />
    )
  }

  return (
    <ModelDetailsRenderer
      className="embedding-model-details"
      details={[
        availabilityDetail(model.availability),
        { label: 'Dimensions', value: String(model.dimensions ?? 'Unknown') },
        { label: 'Mode', value: formatModelMode(model.mode) },
        {
          label: 'Privacy',
          value: privacyLabel(embeddingQueryMayLeaveLocalMachine(model)),
        },
      ]}
    />
  )
}

export function ChatModelDetails({ model }: { model: ChatModel | undefined }) {
  if (!model) {
    return (
      <ModelDetailsRenderer emptyMessage="Model availability, privacy, and runtime requirements appear here after selection." />
    )
  }

  return (
    <ModelDetailsRenderer
      details={[
        availabilityDetail(model.availability),
        { label: 'Mode', value: formatModelMode(model.mode) },
        {
          label: 'Privacy',
          value: privacyLabel(model.promptsMayLeaveLocalMachine),
        },
      ]}
      description={model.description}
    />
  )
}

function ModelDetailsRenderer({
  className = '',
  details = [],
  description,
  emptyMessage,
}: {
  className?: string
  details?: Detail[]
  description?: string | null
  emptyMessage?: string
}) {
  if (emptyMessage) {
    return (
      <div className={`model-details ${className} model-details-empty`}>
        {emptyMessage}
      </div>
    )
  }

  return (
    <div className={`model-details ${className}`}>
      {details.map((detail) => (
        <div key={detail.label}>
          <span>{detail.label}</span>
          {detail.availability ? (
            <AvailabilityValue availability={detail.availability} />
          ) : (
            <strong>{detail.value}</strong>
          )}
        </div>
      ))}
      {description && <p>{description}</p>}
    </div>
  )
}

function AvailabilityValue({ availability }: { availability: string }) {
  return (
    <strong className="availability-value">
      <span
        aria-hidden="true"
        className={`availability-dot availability-dot-${availabilityTone(availability)}`}
      />
      {formatModelAvailability(availability)}
    </strong>
  )
}

function availabilityDetail(availability: string): Detail {
  return {
    label: 'Availability',
    value: formatModelAvailability(availability),
    availability,
  }
}

function privacyLabel(mayLeaveLocalMachine: boolean): string {
  return mayLeaveLocalMachine
    ? 'Prompts may leave this machine'
    : 'Prompts stay local'
}
