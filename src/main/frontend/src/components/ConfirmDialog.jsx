import { Modal, Button } from 'react-bootstrap'

export default function ConfirmDialog({ show, title, message, confirmLabel = 'Confirm', onConfirm, onCancel, danger }) {
  return (
    <Modal show={show} centered backdrop="static" onHide={onCancel}>
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">{title || 'Please confirm'}</Modal.Title>
      </Modal.Header>
      <Modal.Body>{message}</Modal.Body>
      <Modal.Footer>
        <Button variant="light" onClick={onCancel}>
          Cancel
        </Button>
        <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  )
}
