import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { businessWalletApi } from "@/api/wallet";
import { extractErrorMessage } from "@/api/client";
import { useToast } from "@/components/Toast";
import { Button } from "@/components/Button";
import { Input, TextArea } from "@/components/Field";
import { XIcon } from "@/components/icons";

const schema = z.object({
  amountInr: z.coerce.number().positive("Enter an amount greater than zero"),
  note: z.string().max(500).optional(),
});
type FormValues = z.infer<typeof schema>;

export function AddMoneyModal({ businessId, onClose }: { businessId: number; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { push } = useToast();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => businessWalletApi.addMoney(businessId, values),
    onSuccess: () => {
      push("Recorded — awaiting admin confirmation", "success");
      queryClient.invalidateQueries({ queryKey: ["business", "wallet"] });
      onClose();
    },
    onError: (err) => push(extractErrorMessage(err), "error"),
  });

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 p-4">
      <div className="card-surface flex w-full max-w-md flex-col gap-4 p-5">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold text-ink-900">Add money</h2>
          <button
            type="button"
            onClick={onClose}
            className="focus-ring rounded-full p-1 text-ink-400 hover:bg-surface-hover hover:text-ink-700"
            aria-label="Close"
          >
            <XIcon className="h-5 w-5" />
          </button>
        </div>

        <p className="text-sm text-ink-500">
          Record funds you've sent to Escobar. This does not initiate a real payment — an admin confirms the amount before it
          becomes available.
        </p>

        <form onSubmit={handleSubmit((values) => mutation.mutate(values))} className="flex flex-col gap-4">
          <Input
            label="Amount (INR)"
            type="number"
            step="0.01"
            min={0.01}
            error={errors.amountInr?.message}
            {...register("amountInr")}
          />
          <TextArea label="Note (optional)" placeholder="e.g. Campaign funding" rows={2} {...register("note")} />

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={onClose} disabled={mutation.isPending}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting || mutation.isPending}>
              Add money
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
