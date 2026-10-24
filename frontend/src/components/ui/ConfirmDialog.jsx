import Button from './Button';
import Modal, { ModalBody, ModalFooter } from './Modal';

/** "Are you sure?" dialog. Name the action in confirmLabel ("Delete subject"), not "OK". */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Delete',
  cancelLabel = 'Cancel',
  tone = 'danger',
  loading = false,
  confirmDisabled = false,
  onConfirm,
  onCancel,
  children,
}) {
  return (
    <Modal open={open} onClose={loading ? undefined : onCancel} title={title} size="sm" dismissible={!loading}>
      <ModalBody>
        {message && <p className="text-[15px] text-muted">{message}</p>}
        {children}
      </ModalBody>
      <ModalFooter>
        <Button variant="secondary" onClick={onCancel} disabled={loading}>
          {cancelLabel}
        </Button>
        <Button
          variant={tone === 'danger' ? 'danger' : 'primary'}
          onClick={onConfirm}
          loading={loading}
          disabled={confirmDisabled}
          data-autofocus
        >
          {confirmLabel}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
