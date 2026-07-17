import {
  BadgeCheck,
  Blocks,
  BookKey,
  Building2,
  Camera,
  CircleHelp,
  ClipboardList,
  KeyRound,
  LayoutDashboard,
  LayoutGrid,
  MoonStar,
  Puzzle,
  Settings,
  Settings2,
  ShieldCheck,
  SlidersHorizontal,
  SunMedium,
  TrendingUp,
  Users
} from 'lucide-vue-next'

const iconMap = {
  BadgeCheck,
  Blocks,
  BookKey,
  Building2,
  Camera,
  CircleHelp,
  ClipboardList,
  KeyRound,
  LayoutDashboard,
  LayoutGrid,
  MoonStar,
  Puzzle,
  Settings,
  Settings2,
  ShieldCheck,
  SlidersHorizontal,
  SunMedium,
  TrendingUp,
  Users
}

export function resolveIconComponent(iconName) {
  return iconMap[iconName] ?? CircleHelp
}
