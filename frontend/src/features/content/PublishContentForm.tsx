import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { ContentRecord } from "@/types";
import { Button } from "@/components/Button";
import { Input } from "@/components/Field";
import { contentApi } from "@/api/content";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";

const INSTAGRAM_URL_PATTERN = /^https?:\/\/(www\.)?instagram\.com\/(p|reel|reels)\/[A-Za-z0-9_-]+\/?(\?.*)?$/;

export function PublishContentForm({ content }: { content: ContentRecord }) {
  const [postUrl, setPostUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const { push } = useToast();

  const mutation = useMutation({
    mutationFn: () => contentApi.publish(content.id, postUrl),
    onSuccess: () => {
      push("Link submitted for admin review!", "success");
      queryClient.invalidateQueries({ queryKey: ["content"] });
    },
    onError: (err) => setError(extractErrorMessage(err, "Could not publish this content")),
  });

  function handleSubmit() {
    setError(null);
    if (!INSTAGRAM_URL_PATTERN.test(postUrl.trim())) {
      setError("Enter a valid Instagram post or reel URL");
      return;
    }
    mutation.mutate();
  }

  return (
    <div className="border-t border-ink-100 pt-4">
      <div className="flex flex-col gap-3">
        <p className="text-sm text-ink-600">
          Approved! Post this content to Instagram from your own account, then paste the live link here for admin
          review. It goes live once an admin approves it.
        </p>
        {error && <div className="rounded-lg border border-danger-200 bg-danger-soft px-3 py-2 text-sm text-danger-deep">{error}</div>}
        <Input
          placeholder="https://www.instagram.com/p/…"
          value={postUrl}
          onChange={(e) => setPostUrl(e.target.value)}
        />
        <Button isLoading={mutation.isPending} onClick={handleSubmit} variant="gold" className="self-start">
          Submit link for review
        </Button>
      </div>
    </div>
  );
}
