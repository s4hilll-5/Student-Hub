import { useMemo } from 'react';
import { useTheme } from '../../context/ThemeContext';

/**
 * Chart colours, validated for colour-vision deficiency with the dataviz palette
 * validator (adjacent CVD delta E >= 28 in both themes). Recharts needs concrete colour
 * strings, so they are resolved per theme here instead of through CSS variables.
 */
const PALETTE = {
  light: {
    series1: '#4f46e5', // indigo (brand)
    series2: '#eb6834', // orange
    grid: '#e3e1ee',
    axis: '#625f80',
    ink: '#1c1a33',
    surface: '#ffffff',
    goal: '#1c1a33',
  },
  dark: {
    series1: '#7477f2',
    series2: '#d95926',
    grid: '#2b2942',
    axis: '#a3a1c2',
    ink: '#ecebf7',
    surface: '#181726',
    goal: '#ecebf7',
  },
};

export function useChartColors() {
  const { theme } = useTheme();
  return useMemo(() => PALETTE[theme === 'dark' ? 'dark' : 'light'], [theme]);
}
