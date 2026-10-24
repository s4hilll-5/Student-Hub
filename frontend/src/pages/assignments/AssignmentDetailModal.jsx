import { Check, Pencil, Undo2 } from 'lucide-react';
import Badge, { SubjectTag } from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import Modal, { ModalBody, ModalFooter } from '../../components/ui/Modal';
import { ASSIGNMENT_STATUSES, labelOf, PRIORITIES, toneOf } from '../../utils/constants';
import { describeDue, formatDateTime } from '../../utils/date';

function Row({ label, children }) {
  return (
    <div className="grid grid-cols-[110px_1fr] gap-3 text-sm">
      <dt className="text-muted">{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

export default function AssignmentDetailModal({ assignment, onClose, onEdit, onToggleComplete, busy }) {
  const open = Boolean(assignment);
  const completed = assignment?.status === 'COMPLETED';
  return (
    <Modal open={open} onClose={onClose} title={assignment?.title ?? ''}>
      {assignment && (
        <>
          <ModalBody>
            <dl className="space-y-3">
              <Row label="Subject">
                <SubjectTag subject={assignment.subject} />
              </Row>
              <Row label="Due">
                <span>{formatDateTime(assignment.dueAt)}</span>
                <span className={`block text-[13px] ${assignment.overdue ? 'text-rose-600 dark:text-rose-400' : 'text-muted'}`}>
                  {completed ? 'Completed' : describeDue(assignment.dueAt)}
                </span>
              </Row>
              <Row label="Priority">
                <Badge tone={toneOf(PRIORITIES, assignment.priority)}>{labelOf(PRIORITIES, assignment.priority)}</Badge>
              </Row>
              <Row label="Status">
                <Badge tone={toneOf(ASSIGNMENT_STATUSES, assignment.status)}>{labelOf(ASSIGNMENT_STATUSES, assignment.status)}</Badge>
              </Row>
              {assignment.completedAt && <Row label="Completed">{formatDateTime(assignment.completedAt)}</Row>}
              <Row label="Added">{formatDateTime(assignment.createdAt)}</Row>
              <Row label="Last edited">{formatDateTime(assignment.updatedAt)}</Row>
            </dl>
            {assignment.description && (
              <div className="rounded-xl bg-subtle p-4 text-[15px] leading-relaxed whitespace-pre-wrap">{assignment.description}</div>
            )}
          </ModalBody>
          <ModalFooter>
            <Button variant="secondary" icon={Pencil} onClick={() => onEdit(assignment)}>
              Edit
            </Button>
            <Button icon={completed ? Undo2 : Check} loading={busy} onClick={() => onToggleComplete(assignment)}>
              {completed ? 'Mark as not done' : 'Mark as completed'}
            </Button>
          </ModalFooter>
        </>
      )}
    </Modal>
  );
}
