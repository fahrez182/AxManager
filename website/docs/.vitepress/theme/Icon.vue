<script setup lang="ts">
// <Icon name="terminal" /> in markdown; the names are in icons.ts.
import { icons } from "./icons";

defineProps<{ name: string }>();
</script>

<template>
  <svg class="ax-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path :d="icons[name]" />
  </svg>
</template>
