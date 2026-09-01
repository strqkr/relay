import Link from 'next/link';

export default function HomePage() {
  return (
    <div className="flex flex-col justify-center text-center flex-1 gap-3">
      <h1 className="text-2xl font-bold">relay</h1>
      <p className="text-fd-muted-foreground">
        A multi-tenant webhook delivery service — publish events to topics, fan them out
        to subscribed endpoints, and retry failures automatically.
      </p>
      <p>
        <Link href="/docs" className="font-medium underline">
          Read the docs
        </Link>
      </p>
    </div>
  );
}
