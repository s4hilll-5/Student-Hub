/**
 * Plain-language reading of the attendance stats the API calculates.
 * calculateAttendance mirrors the backend formula; the app uses the server's numbers
 * everywhere, and the client copy only powers the interactive demo on the landing page.
 */

export function calculateAttendance(present, total, target) {
  const absent = Math.max(total - present, 0);
  const counted = present + absent;
  const percentage = counted === 0 ? null : Math.round((present / counted) * 10000) / 100;
  const meetsTarget = counted === 0 || present * 100 >= target * counted;
  let needed = 0;
  let reachable = true;
  if (!meetsTarget) {
    if (target >= 100) {
      needed = null;
      reachable = false;
    } else {
      needed = Math.ceil((target * counted - 100 * present) / (100 - target) - 1e-9);
    }
  }
  const canMiss = meetsTarget ? Math.floor((100 * present - target * counted) / target + 1e-9) : 0;
  return {
    present,
    absent,
    totalCounted: counted,
    percentage,
    target,
    meetsTarget,
    classesNeededToReachTarget: needed,
    targetReachable: reachable,
    classesYouCanMiss: canMiss,
  };
}

const plural = (n, word) => `${n} ${word}${n === 1 ? '' : 'es'}`;

/** { tone, text } describing where the student stands against the target. */
export function attendanceVerdict(stats) {
  if (!stats || stats.totalCounted === 0) {
    return { tone: 'neutral', text: 'No classes recorded yet' };
  }
  if (!stats.targetReachable) {
    return { tone: 'danger', text: `${Number(stats.target)}% can't be reached any more` };
  }
  if (!stats.meetsTarget) {
    return {
      tone: 'danger',
      text: `Attend the next ${plural(stats.classesNeededToReachTarget, 'class')} to reach ${Number(stats.target)}%`,
    };
  }
  if (stats.classesYouCanMiss === 0) {
    return { tone: 'warning', text: 'On target, with no classes to spare' };
  }
  return { tone: 'success', text: `You can miss ${plural(stats.classesYouCanMiss, 'class')} and stay on target` };
}

export function formatPercent(value, digits = 1) {
  if (value === null || value === undefined) return '—';
  return `${Number(value).toFixed(digits).replace(/\.0+$/, '')}%`;
}
