// Route numbers styled like the destination board on the front of a bus.
export default function RouteChip({ number }) {
  return (
    <span className="board inline-block rounded-sm bg-board px-2 py-0.5 text-sm text-amber">
      {number}
    </span>
  );
}
